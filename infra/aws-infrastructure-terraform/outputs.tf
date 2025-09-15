# VPC Outputs
output "vpc_id" {
  description = "ID of the VPC"
  value       = module.vpc.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = module.vpc.vpc_cidr_block
}

output "private_subnet_ids" {
  description = "List of IDs of the private subnets"
  value       = module.vpc.private_subnet_ids
}

output "public_subnet_ids" {
  description = "List of IDs of the public subnets"
  value       = module.vpc.public_subnet_ids
}

output "database_subnet_ids" {
  description = "List of IDs of the database subnets"
  value       = module.vpc.database_subnet_ids
}

# EKS Outputs
output "cluster_id" {
  description = "The ID of the EKS cluster"
  value       = module.eks.cluster_id
}

output "cluster_arn" {
  description = "The Amazon Resource Name (ARN) of the cluster"
  value       = module.eks.cluster_arn
}

output "cluster_endpoint" {
  description = "The endpoint for your EKS Kubernetes API"
  value       = module.eks.cluster_endpoint
}

output "cluster_version" {
  description = "The Kubernetes server version for the EKS cluster"
  value       = module.eks.cluster_version
}

output "cluster_certificate_authority_data" {
  description = "Base64 encoded certificate data required to communicate with the cluster"
  value       = module.eks.cluster_certificate_authority_data
}

output "cluster_oidc_issuer_url" {
  description = "The URL on the EKS cluster OIDC Issuer"
  value       = module.eks.cluster_oidc_issuer_url
}

output "oidc_provider_arn" {
  description = "The ARN of the OIDC Identity Provider if enabled"
  value       = module.eks.oidc_provider_arn
}

output "cluster_security_group_id" {
  description = "Security group ID attached to the EKS cluster"
  value       = module.eks.cluster_security_group_id
}

# RDS Outputs
output "db_instance_endpoint" {
  description = "RDS instance endpoint"
  value       = module.rds.db_instance_endpoint
}

output "db_instance_address" {
  description = "RDS instance hostname"
  value       = module.rds.db_instance_address
}

output "db_instance_port" {
  description = "RDS instance port"
  value       = module.rds.db_instance_port
}

output "db_instance_name" {
  description = "RDS instance database name"
  value       = module.rds.db_instance_name
}

output "db_master_user_secret_arn" {
  description = "ARN of the master user secret when using AWS managed master user password"
  value       = module.rds.db_master_user_secret_arn
}

# Redis Outputs
output "redis_primary_endpoint_address" {
  description = "Address of the endpoint for the primary node in the replication group"
  value       = module.redis.redis_primary_endpoint_address
}

output "redis_port" {
  description = "Redis port"
  value       = module.redis.redis_port
}

output "redis_connection_info" {
  description = "Redis connection information"
  value       = module.redis.redis_connection_info
}

# Kubectl Configuration Command
output "kubectl_config_command" {
  description = "Command to configure kubectl"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_id}"
}

# Connection Information for Applications
output "application_config" {
  description = "Configuration information for applications"
  value = {
    database = {
      host                = module.rds.db_instance_address
      port                = module.rds.db_instance_port
      database_name       = module.rds.db_instance_name
      secret_arn          = module.rds.db_master_user_secret_arn
    }
    redis = {
      host = module.redis.redis_connection_info.host
      port = module.redis.redis_connection_info.port
      ssl_enabled = module.redis.redis_connection_info.ssl_enabled
    }
    kubernetes = {
      cluster_name        = module.eks.cluster_id
      cluster_endpoint    = module.eks.cluster_endpoint
      oidc_issuer_url     = module.eks.cluster_oidc_issuer_url
      oidc_provider_arn   = module.eks.oidc_provider_arn
    }
  }
  sensitive = true
}
