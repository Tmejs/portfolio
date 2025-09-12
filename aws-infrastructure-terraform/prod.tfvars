# Production Environment Configuration
environment = "prod"

# Networking
aws_region = "us-west-2"
availability_zones = ["us-west-2a", "us-west-2b", "us-west-2c"]
vpc_cidr = "10.1.0.0/16"
public_subnet_cidrs = ["10.1.101.0/24", "10.1.102.0/24", "10.1.103.0/24"]
private_subnet_cidrs = ["10.1.1.0/24", "10.1.2.0/24", "10.1.3.0/24"]
database_subnet_cidrs = ["10.1.201.0/24", "10.1.202.0/24", "10.1.203.0/24"]

# EKS Configuration
kubernetes_version = "1.28"
eks_node_instance_types = ["t3.medium", "t3.large"]
eks_capacity_type = "ON_DEMAND"
eks_node_scaling_config = {
  desired_size = 3
  max_size     = 10
  min_size     = 2
}
eks_endpoint_private_access = true
eks_endpoint_public_access = true
eks_public_access_cidrs = ["0.0.0.0/0"]  # Consider restricting this in real prod

# RDS Configuration (production-ready)
rds_instance_class = "db.t3.small"
rds_allocated_storage = 100
rds_max_allocated_storage = 500
rds_engine_version = "15.4"
rds_backup_retention_period = 14
rds_multi_az = true
rds_performance_insights_enabled = true
rds_monitoring_interval = 60
rds_manage_master_user_password = true

# Redis Configuration (production-ready)
redis_node_type = "cache.t3.small"
redis_num_cache_clusters = 2
redis_engine_version = "7.0"
redis_at_rest_encryption_enabled = true
redis_transit_encryption_enabled = true
redis_automatic_failover_enabled = true
redis_multi_az_enabled = true
redis_snapshot_retention_limit = 14

# Application deployment
enable_monitoring = true
deploy_application = true
