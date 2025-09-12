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
