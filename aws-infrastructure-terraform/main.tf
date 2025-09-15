terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.1"
    }
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Environment = var.environment
      Project     = "portfolio-microservices"
      ManagedBy   = "terraform"
    }
  }
}

# Helm provider configuration
provider "helm" {
  kubernetes {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
    
    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args = [
        "eks",
        "get-token",
        "--cluster-name",
        module.eks.cluster_id,
        "--region",
        var.aws_region
      ]
    }
  }
}

# Kubernetes provider configuration
provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
  
  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args = [
      "eks",
      "get-token",
      "--cluster-name",
      module.eks.cluster_id,
      "--region",
      var.aws_region
    ]
  }
}

# Data sources
data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}

# Local values
locals {
  cluster_name = "${var.project_name}-${var.environment}-eks"
  common_tags = {
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "terraform"
  }
}

# VPC Module
module "vpc" {
  source = "./modules/vpc"

  project_name             = var.project_name
  environment              = var.environment
  vpc_cidr                 = var.vpc_cidr
  availability_zones       = var.availability_zones
  public_subnet_cidrs      = var.public_subnet_cidrs
  private_subnet_cidrs     = var.private_subnet_cidrs
  database_subnet_cidrs    = var.database_subnet_cidrs

  tags = local.common_tags
}

# EKS Module
module "eks" {
  source = "./modules/eks"

  cluster_name       = local.cluster_name
  kubernetes_version = var.kubernetes_version
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  public_subnet_ids  = module.vpc.public_subnet_ids

  instance_types = var.eks_node_instance_types
  scaling_config = var.eks_node_scaling_config
  capacity_type  = var.eks_capacity_type

  endpoint_private_access = var.eks_endpoint_private_access
  endpoint_public_access  = var.eks_endpoint_public_access
  public_access_cidrs     = var.eks_public_access_cidrs

  enable_irsa = true

  tags = local.common_tags

  depends_on = [module.vpc]
}

# External Secrets Operator
resource "helm_release" "external_secrets" {
  name       = "external-secrets"
  repository = "https://charts.external-secrets.io"
  chart      = "external-secrets"
  version    = "0.9.11"
  namespace  = "external-secrets"

  create_namespace = true

  set {
    name  = "installCRDs"
    value = "true"
  }

  set {
    name  = "serviceAccount.annotations.eks\.amazonaws\.com/role-arn"
    value = module.eks.external_secrets_service_account_role_arn
  }

  depends_on = [module.eks]
}

# AWS Secrets Manager SecretStore
resource "kubernetes_manifest" "secret_store" {
  manifest = {
    apiVersion = "external-secrets.io/v1beta1"
    kind       = "SecretStore"
    metadata = {
      name      = "aws-secrets-manager"
      namespace = "default"
    }
    spec = {
      provider = {
        aws = {
          service = "SecretsManager"
          region  = var.aws_region
          auth = {
            serviceAccount = {
              name = "external-secrets"
            }
          }
        }
      }
    }
  }

  depends_on = [helm_release.external_secrets]
}

# Prometheus Stack (kube-prometheus-stack)
resource "helm_release" "prometheus_stack" {
  count = var.enable_monitoring ? 1 : 0

  name       = "prometheus"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  version    = "55.5.0"
  namespace  = "monitoring"

  create_namespace = true
  timeout          = 600

  values = [
    yamlencode({
      prometheus = {
        prometheusSpec = {
          serviceMonitorSelectorNilUsesHelmValues = false
          podMonitorSelectorNilUsesHelmValues    = false
          retention                               = "15d"
          storageSpec = {
            volumeClaimTemplate = {
              spec = {
                storageClassName = "gp2"
                accessModes      = ["ReadWriteOnce"]
                resources = {
                  requests = {
                    storage = "20Gi"
                  }
                }
              }
            }
          }
        }
      }
      grafana = {
        adminPassword = "admin123"  # Change this in production!
        persistence = {
          enabled          = true
          storageClassName = "gp2"
          size             = "10Gi"
        }
        dashboardProviders = {
          "dashboardproviders.yaml" = {
            apiVersion = 1
            providers = [
              {
                name            = "default"
                orgId           = 1
                folder          = ""
                type            = "file"
                disableDeletion = false
                editable        = true
                options = {
                  path = "/var/lib/grafana/dashboards/default"
                }
              }
            ]
          }
        }
      }
      alertmanager = {
        alertmanagerSpec = {
          storage = {
            volumeClaimTemplate = {
              spec = {
                storageClassName = "gp2"
                accessModes      = ["ReadWriteOnce"]
                resources = {
                  requests = {
                    storage = "5Gi"
                  }
                }
              }
            }
          }
        }
      }
    })
  ]

  depends_on = [module.eks]
}

# Banking Microservice Application Deployment
resource "helm_release" "banking_microservice" {
  count = var.deploy_application ? 1 : 0

  name      = "banking-microservice"
  chart     = "../helm-charts/banking-microservice"
  namespace = "default"

  values = [
    file("../helm-charts/banking-microservice/values-${var.environment}.yaml")
  ]

  set_string {
    name  = "database.host"
    value = module.rds.db_instance_address
  }

  set_string {
    name  = "redis.host"
    value = module.redis.redis_primary_endpoint_address
  }

  set_string {
    name  = "serviceAccount.annotations.eks\.amazonaws\.com/role-arn"
    value = module.eks.external_secrets_service_account_role_arn
  }

  depends_on = [
    module.rds,
    module.redis,
    helm_release.external_secrets,
    kubernetes_manifest.secret_store
  ]
}

# RDS Module
module "rds" {
  source = "./modules/rds"

  project_name         = var.project_name
  environment          = var.environment
  vpc_id               = module.vpc.vpc_id
  database_subnet_ids  = module.vpc.database_subnet_ids

  # Allow access from EKS nodes
  allowed_security_groups = [module.eks.cluster_security_group_id]

  # Database configuration
  db_instance_class         = var.rds_instance_class
  db_allocated_storage      = var.rds_allocated_storage
  db_max_allocated_storage  = var.rds_max_allocated_storage
  db_engine_version         = var.rds_engine_version
  backup_retention_period   = var.rds_backup_retention_period
  multi_az                  = var.rds_multi_az
  performance_insights_enabled = var.rds_performance_insights_enabled
  monitoring_interval       = var.rds_monitoring_interval
  manage_master_user_password = var.rds_manage_master_user_password

  tags = local.common_tags

  depends_on = [module.vpc]
}

# Redis Module
module "redis" {
  source = "./modules/redis"

  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids

  # Allow access from EKS nodes
  allowed_security_groups = [module.eks.cluster_security_group_id]

  # Redis configuration
  node_type                   = var.redis_node_type
  num_cache_clusters          = var.redis_num_cache_clusters
  engine_version              = var.redis_engine_version
  at_rest_encryption_enabled  = var.redis_at_rest_encryption_enabled
  transit_encryption_enabled  = var.redis_transit_encryption_enabled
  automatic_failover_enabled  = var.redis_automatic_failover_enabled
  multi_az_enabled           = var.redis_multi_az_enabled
  snapshot_retention_limit    = var.redis_snapshot_retention_limit

  tags = local.common_tags

  depends_on = [module.vpc]
}
