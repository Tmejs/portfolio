# LocalStack Configuration Variables
# Use these settings for local development with LocalStack

aws_region    = "us-west-2"
environment   = "localstack"
project_name  = "portfolio-microservices-local"

# VPC Configuration (smaller for local testing)
vpc_cidr = "10.0.0.0/16"
availability_zones = ["us-west-2a", "us-west-2b"]

# Smaller subnets for local development
private_subnet_cidrs   = ["10.0.1.0/24", "10.0.2.0/24"]
public_subnet_cidrs    = ["10.0.101.0/24", "10.0.102.0/24"]
database_subnet_cidrs  = ["10.0.201.0/24", "10.0.202.0/24"]

# Minimal resources for local testing
eks_node_instance_types = ["t3.micro"]
eks_node_scaling_config = {
  desired_size = 1
  max_size     = 2
  min_size     = 1
}

# Smallest possible instances for local development
rds_instance_class = "db.t3.micro"
redis_node_type    = "cache.t3.micro"
