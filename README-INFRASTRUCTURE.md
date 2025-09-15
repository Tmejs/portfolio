# Banking Microservice Infrastructure

This repository contains a comprehensive infrastructure setup for deploying a Spring Boot banking microservice on AWS EKS with PostgreSQL, Redis, and monitoring.

## Architecture Overview

The infrastructure consists of:

- **AWS VPC**: Multi-AZ networking with public, private, and database subnets
- **EKS Cluster**: Kubernetes cluster with OIDC provider for service accounts
- **RDS PostgreSQL**: Database with encryption, backups, and secrets management
- **ElastiCache Redis**: In-memory caching with encryption and high availability
- **External Secrets Operator**: AWS Secrets Manager integration
- **Prometheus/Grafana**: Monitoring and observability stack
- **Helm Charts**: Application deployment with environment-specific configurations

## Prerequisites

- AWS CLI configured with appropriate permissions
- Terraform >= 1.6
- kubectl
- Helm >= 3.0
- Java 24 with preview features (for local development)

## Directory Structure

```
├── aws-infrastructure-terraform/     # Terraform infrastructure code
│   ├── modules/                     # Reusable Terraform modules
│   │   ├── vpc/                    # VPC module
│   │   ├── eks/                    # EKS cluster module
│   │   ├── rds/                    # PostgreSQL database module
│   │   └── redis/                  # ElastiCache Redis module
│   ├── dev.tfvars                  # Development environment config
│   ├── prod.tfvars                 # Production environment config
│   └── *.tf                        # Main Terraform configuration
├── helm-charts/                     # Kubernetes deployment charts
│   └── banking-microservice/       # Banking app Helm chart
└── spring-boot-microservices-demo/ # Spring Boot application code
```

## Infrastructure Deployment

### 1. Deploy Development Environment

```bash
cd aws-infrastructure-terraform

# Initialize Terraform
terraform init

# Plan the deployment
terraform plan -var-file="dev.tfvars"

# Apply the infrastructure
terraform apply -var-file="dev.tfvars"
```

### 2. Configure kubectl

```bash
# Get the kubectl configuration command from Terraform output
terraform output kubectl_config_command

# Run the command (example)
aws eks update-kubeconfig --region us-west-2 --name portfolio-microservices-dev-eks
```

### 3. Deploy Production Environment

```bash
# Use production variables
terraform plan -var-file="prod.tfvars"
terraform apply -var-file="prod.tfvars"
```

## Application Deployment

The infrastructure automatically deploys the banking microservice when `deploy_application = true` is set in the tfvars file.

### Manual Deployment

If you prefer manual deployment:

```bash
# Navigate to helm chart directory
cd helm-charts/banking-microservice

# Deploy to development
helm install banking-microservice . \
  -f values-dev.yaml \
  --set-string database.host=<RDS_ENDPOINT> \
  --set-string redis.host=<REDIS_ENDPOINT> \
  --set-string serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn=<IRSA_ROLE_ARN>

# Deploy to production
helm install banking-microservice . \
  -f values-prod.yaml \
  --set-string database.host=<RDS_ENDPOINT> \
  --set-string redis.host=<REDIS_ENDPOINT> \
  --set-string serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn=<IRSA_ROLE_ARN>
```

## Configuration Management

### Environment Variables

The application uses the following environment variables (managed via Helm):

- `SPRING_DATASOURCE_URL`: PostgreSQL connection string
- `DB_USERNAME`/`DB_PASSWORD`: Database credentials (from AWS Secrets Manager)
- `REDIS_HOST`/`REDIS_PORT`: Redis connection details
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka cluster endpoints

### Secrets Management

Database credentials are automatically managed via:

1. **RDS**: AWS Secrets Manager integration for master user password
2. **External Secrets Operator**: Syncs secrets from AWS Secrets Manager to Kubernetes
3. **IRSA**: IAM Roles for Service Accounts for secure access

### Environment-Specific Configuration

| Environment | Resources | Features |
|-------------|-----------|----------|
| **Dev** | Cost-optimized (t3.small, single-AZ) | Basic monitoring, simplified security |
| **Prod** | High-availability (multi-AZ, autoscaling) | Full monitoring, strict security, backups |

## Monitoring and Observability

### Prometheus/Grafana Stack

When `enable_monitoring = true`, the infrastructure deploys:

- **Prometheus**: Metrics collection and storage
- **Grafana**: Visualization dashboards (admin/admin123)
- **AlertManager**: Alert routing and management

Access Grafana:
```bash
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80
```

### Application Metrics

The banking microservice exposes metrics at `/actuator/prometheus` for:

- JVM metrics (memory, threads, GC)
- HTTP request metrics
- Database connection pool metrics
- Custom business metrics

### Health Checks

- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`

## Database Management

### Schema Management

The application uses Flyway for database migrations:

- Location: `src/main/resources/db/migration`
- Schema: `banking` (configured in both Hibernate and Flyway)
- Auto-migration on startup

### Connection Details

```yaml
Database: portfolio_banking
Schema: banking
Port: 5432
SSL: Required (in production)
```

## Security

### Network Security

- Private subnets for EKS nodes and databases
- Security groups with minimal required access
- No public access to databases

### IAM and RBAC

- **IRSA**: Service accounts use IAM roles for AWS access
- **Least Privilege**: Minimal required permissions
- **Encryption**: At-rest and in-transit encryption enabled

### Secrets Management

- Database passwords managed by AWS RDS
- Application secrets stored in AWS Secrets Manager
- Kubernetes secrets synced via External Secrets Operator

## Cost Optimization

### Development Environment

- Single-AZ deployment
- Smaller instance types (t3.micro, t3.small)
- Shorter backup retention (3 days)
- Basic monitoring only

### Production Environment

- Multi-AZ for high availability
- Larger instance types for performance
- Extended backup retention (14 days)
- Full monitoring and alerting

## Troubleshooting

### Common Issues

1. **EKS Access Denied**
   ```bash
   # Ensure AWS CLI is configured correctly
   aws sts get-caller-identity
   
   # Update kubeconfig
   aws eks update-kubeconfig --region us-west-2 --name <cluster-name>
   ```

2. **Database Connection Issues**
   ```bash
   # Check security groups and network connectivity
   kubectl get secrets banking-db-secret -o yaml
   kubectl logs deployment/banking-microservice
   ```

3. **External Secrets Not Working**
   ```bash
   # Check external secrets operator
   kubectl get pods -n external-secrets
   kubectl logs -n external-secrets deployment/external-secrets
   ```

### Useful Commands

```bash
# Check infrastructure status
terraform show

# Get connection information
terraform output application_config

# Monitor pod logs
kubectl logs -f deployment/banking-microservice

# Check service health
kubectl get pods,svc,ingress

# Access Grafana
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80

# Scale application
kubectl scale deployment banking-microservice --replicas=3
```

## Cleanup

To destroy the infrastructure:

```bash
cd aws-infrastructure-terraform

# Destroy resources
terraform destroy -var-file="dev.tfvars"  # or prod.tfvars
```

**Note**: This will delete all resources including databases. Ensure you have backups if needed.

## Next Steps

1. **CI/CD Pipeline**: Integrate with GitHub Actions or AWS CodePipeline
2. **Service Mesh**: Consider Istio for advanced traffic management
3. **Backup Strategy**: Implement automated database and application backups
4. **Disaster Recovery**: Set up cross-region replication
5. **Security Scanning**: Integrate container and infrastructure security scanning

## Support

For questions or issues:

1. Check the application logs: `kubectl logs -f deployment/banking-microservice`
2. Review Terraform state: `terraform show`
3. Check AWS resources in the console
4. Verify Helm deployments: `helm list -A`

---

This infrastructure setup provides a production-ready foundation for deploying Spring Boot microservices on AWS with best practices for security, monitoring, and scalability.
