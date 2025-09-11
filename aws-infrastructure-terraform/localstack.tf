# LocalStack Provider Configuration
# This file configures Terraform to work with LocalStack for local development

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# AWS Provider for LocalStack
provider "aws" {
  region                      = var.aws_region
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  # LocalStack endpoints
  endpoints {
    ec2            = "http://localhost:4566"
    ecs            = "http://localhost:4566"
    eks            = "http://localhost:4566"
    iam            = "http://localhost:4566"
    rds            = "http://localhost:4566"
    elasticache    = "http://localhost:4566"
    logs           = "http://localhost:4566"
    events         = "http://localhost:4566"
    lambda         = "http://localhost:4566"
    s3             = "http://localhost:4566"
    secretsmanager = "http://localhost:4566"
  }

  default_tags {
    tags = {
      Environment = var.environment
      Project     = "portfolio-microservices-local"
      ManagedBy   = "terraform"
      LocalStack  = "true"
    }
  }
}

# Use this configuration for LocalStack by running:
# export TF_VAR_use_localstack=true
# terraform init
# terraform plan -var-file="localstack.tfvars"
# terraform apply -var-file="localstack.tfvars"
