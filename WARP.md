# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

Repository overview
- spring-boot-microservices-demo: Spring Boot 3.3.4 banking application targeting Java 24 (preview features enabled). Integrates PostgreSQL (JPA/Flyway) with banking schema, Redis, Kafka, Micrometer/Prometheus, OpenAPI, Testcontainers.
- aws-infrastructure-terraform: Terraform IaC to provision AWS networking and compute (VPC, EKS), with local development flow via LocalStack.

Common commands
- Spring Boot app (spring-boot-microservices-demo)
  - Build
    - mvn -f spring-boot-microservices-demo/pom.xml clean compile
  - Run (dev)
    - mvn -f spring-boot-microservices-demo/pom.xml spring-boot:run
  - Tests
    - Run all tests: mvn -f spring-boot-microservices-demo/pom.xml test
    - Run integration tests (includes Testcontainers): mvn -f spring-boot-microservices-demo/pom.xml verify
    - Run a single test class: mvn -f spring-boot-microservices-demo/pom.xml test -Dtest=UserServiceTest
    - Run a single test method: mvn -f spring-boot-microservices-demo/pom.xml test -Dtest=UserServiceTest#methodName
  - Install artifact locally
    - mvn -f spring-boot-microservices-demo/pom.xml clean install
  - Notes
    - Java 24 preview is enabled via maven-compiler-plugin and surefire (argLine --enable-preview).
    - No explicit linter is configured in pom.xml.

- Infrastructure (aws-infrastructure-terraform)
  - Local development with LocalStack
    - Start LocalStack: docker-compose -f aws-infrastructure-terraform/docker-compose.localstack.yml up -d
    - Health check: curl http://localhost:4566/health
    - Init/plan/apply against LocalStack:
      - (cd aws-infrastructure-terraform && terraform init)
      - terraform -chdir=aws-infrastructure-terraform plan -var-file="localstack.tfvars"
      - terraform -chdir=aws-infrastructure-terraform apply -var-file="localstack.tfvars" -auto-approve
    - Inspect resources (examples):
      - awslocal ec2 describe-vpcs
      - awslocal rds describe-db-instances
      - awslocal elasticache describe-cache-clusters
    - Cleanup:
      - terraform -chdir=aws-infrastructure-terraform destroy -var-file="localstack.tfvars" -auto-approve
      - docker-compose -f aws-infrastructure-terraform/docker-compose.localstack.yml down -v
  - Deploy to AWS
    - terraform -chdir=aws-infrastructure-terraform init
    - terraform -chdir=aws-infrastructure-terraform plan
    - terraform -chdir=aws-infrastructure-terraform apply
    - Configure kubectl for EKS (region/name from README):
      - aws eks update-kubeconfig --region us-west-2 --name portfolio-microservices-dev-eks
      - kubectl get nodes

High-level architecture and structure
- Spring application
  - Layers (from README):
    - Web layer: REST controllers and exception handlers, exposes APIs documented via OpenAPI (springdoc).
    - Service layer: business logic, async processing (@EnableAsync), event publishing.
    - Data layer: JPA repositories backed by PostgreSQL; Redis used for caching.
    - Messaging layer: Kafka producers/consumers (@EnableKafka).
  - Observability: Micrometer with Prometheus registry; Actuator endpoints provide health and metrics.
  - Testing: JUnit + Spring Boot Test; Testcontainers for PostgreSQL and Kafka integration tests (activated during mvn verify).
  - Notable build config (pom.xml):
    - Parent: spring-boot-starter-parent 3.3.4; Java 24 source/target; --enable-preview passed to compiler and Surefire.
    - Dependencies include: spring-boot-starter-web, data-jpa, data-redis, validation, actuator; postgresql driver; flyway; spring-kafka; micrometer-registry-prometheus; springdoc-openapi.
  - Layout (standard Maven):
    - src/main/java/... with application entrypoint at com.portfolio.demo.MicroservicesDemoApplication (annotated with @SpringBootApplication, @EnableAsync, @EnableKafka)
    - src/test/java/... for unit/integration tests.
    - Configuration files referenced in README: application.yml, application-dev.yml, application-prod.yml.

- Terraform infrastructure
  - Providers: aws (~>5), kubernetes (~>2.23), helm (~>2.11); required_version >= 1.6.
  - Core structure and locals:
    - Variables define region, environment, VPC CIDRs, subnets, EKS node types/scaling, and placeholders for RDS/Redis sizing.
    - Locals compute cluster_name and common tags applied via aws provider default_tags.
  - Modules (as described in README and directory tree):
    - vpc/: VPC, public/private/database subnets, routing, NAT/IGW with cost-conscious defaults.
    - eks/: EKS cluster and node group.
    - rds/, redis/: placeholders for database layers (implementation may be added/expanded).
  - LocalStack notes: Community edition has limited support for EKS; full features may require LocalStack Pro.

Important references from READMEs
- Spring Boot README
  - Java 24 setup (sdkman), tech stack, API docs endpoints once running (Swagger UI /v3 docs, Actuator health/metrics), and Docker-based local dependencies (PostgreSQL, Redis, Kafka) guidance.
  - Testing commands including class-specific runs and verify for integration tests.
- Terraform README
  - End-to-end LocalStack flow, AWS deploy flow, and layout/notes regarding cost controls and terraform fmt/validate usage.

Paths and entry points
- Application entry: spring-boot-microservices-demo/src/main/java/com/portfolio/demo/MicroservicesDemoApplication.java
- Terraform root: aws-infrastructure-terraform/

Database architecture
- Uses PostgreSQL with schema-based separation for multi-service architecture
- Banking service operates in 'banking' schema with tables: accounts, transactions
- Flyway manages schema migrations with versioned SQL files
- Future services can use separate schemas (e.g., analytics, audit) with isolated security policies
- Database connection: jdbc:postgresql://localhost:5432/portfolio_banking
- Default schema configured in Hibernate and Flyway: banking

Conventions and expectations for agents
- Prefer project-scoped commands provided above (mvn with -f path and terraform -chdir) to avoid cd state.
- When working with Java preview features, ensure commands that fork the JVM for tests respect the existing pom configuration (Surefire argLine is already set).
- When creating new database entities, always specify schema="banking" in @Table annotation.
- All Flyway migrations should operate within banking schema using SET search_path TO banking.
