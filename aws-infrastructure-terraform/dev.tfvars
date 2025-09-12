# Development Environment Configuration
environment = "dev"

# Networking
aws_region = "us-west-2"
availability_zones = ["us-west-2a", "us-west-2b"]
vpc_cidr = "10.0.0.0/16"
public_subnet_cidrs = ["10.0.101.0/24", "10.0.102.0/24"]
private_subnet_cidrs = ["10.0.1.0/24", "10.0.2.0/24"]
database_subnet_cidrs = ["10.0.201.0/24", "10.0.202.0/24"]

# EKS Configuration
kubernetes_version = "1.28"
eks_node_instance_types = ["t3.small"]
eks_capacity_type = "ON_DEMAND"
eks_node_scaling_config = {
  desired_size = 1
  max_size     = 3
  min_size     = 1
}
eks_endpoint_private_access = true
eks_endpoint_public_access = true
eks_public_access_cidrs = ["0.0.0.0/0"]

# RDS Configuration (cost-optimized for dev)
rds_instance_class = "db.t3.micro"
rds_allocated_storage = 20
rds_max_allocated_storage = 50
rds_engine_version = "15.4"
rds_backup_retention_period = 3
rds_multi_az = false
rds_performance_insights_enabled = false
rds_monitoring_interval = 0
rds_manage_master_user_password = true

# Redis Configuration (cost-optimized for dev)
redis_node_type = "cache.t3.micro"
redis_num_cache_clusters = 1
redis_engine_version = "7.0"
redis_at_rest_encryption_enabled = true
redis_transit_encryption_enabled = false  # Disabled for dev to avoid auth complexity
redis_automatic_failover_enabled = false
redis_multi_az_enabled = false
redis_snapshot_retention_limit = 3
