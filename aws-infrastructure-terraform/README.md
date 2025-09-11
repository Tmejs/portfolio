# AWS Infrastructure with Terraform

Infrastructure for deploying the Spring Boot Microservices Demo on AWS (and locally via LocalStack).

## Quick Start

### Local development with LocalStack

Prerequisites:
- Docker & Docker Compose
- Terraform >= 1.6 (installed to ~/.local/bin in this setup)
- LocalStack CLI (installed): `pip3 install localstack awscli-local`

Steps:
- Start LocalStack
  ```bash
  docker-compose -f docker-compose.localstack.yml up -d
  curl http://localhost:4566/health
  ```
- Initialize & deploy to LocalStack
  ```bash
  terraform init
  terraform plan -var-file="localstack.tfvars"
  terraform apply -var-file="localstack.tfvars" -auto-approve
  ```
- Inspect resources
  ```bash
  awslocal ec2 describe-vpcs
  awslocal rds describe-db-instances
  awslocal elasticache describe-cache-clusters
  ```
- Cleanup
  ```bash
  terraform destroy -var-file="localstack.tfvars" -auto-approve
  docker-compose -f docker-compose.localstack.yml down -v
  ```

### Deploy to AWS (real cloud)

Prerequisites:
- AWS CLI configured (aws configure)
- Permissions for VPC, EKS, RDS, ElastiCache, IAM
- kubectl for EKS

Steps:
```bash
terraform init
terraform plan
terraform apply
aws eks update-kubeconfig --region us-west-2 --name portfolio-microservices-dev-eks
kubectl get nodes
```

## Layout
- main.tf, variables.tf: root configuration
- modules/
  - vpc/: VPC, subnets, routing, NAT/IGW
  - eks/: EKS cluster and node group
  - rds/, redis/: placeholders for DBs
- docker-compose.localstack.yml: LocalStack services
- localstack.tf: Provider override for LocalStack
- localstack.tfvars: LocalStack-specific variables

## Notes
- LocalStack Community has limited EKS support; full features require LocalStack Pro.
- For cost control in AWS, this repo uses a single NAT Gateway by default.
- Use terraform fmt/validate before committing.

