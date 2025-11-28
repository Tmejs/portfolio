# 🏦 Enterprise Banking Microservices Platform

> A production-ready, cloud-native banking platform demonstrating modern Java development, microservices architecture, and DevOps best practices.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Tests](https://img.shields.io/badge/Tests-113%20Passing-success.svg)](./spring-boot-microservices-demo/TEST-COVERAGE.md)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [Services](#-services)
- [Getting Started](#-getting-started)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Project Structure](#-project-structure)
- [Documentation](#-documentation)

---

## 🎯 Overview

This project showcases a **complete enterprise-grade banking platform** built with modern microservices architecture. It demonstrates proficiency in:

- **Backend Development**: Spring Boot 3.x with Java 21
- **Microservices**: Event-driven architecture with Kafka
- **Data Management**: PostgreSQL, Redis, MongoDB
- **Cloud Infrastructure**: AWS EKS with Terraform IaC
- **DevOps**: CI/CD pipelines, Docker, Kubernetes, Helm
- **Testing**: Comprehensive unit and integration tests (113 tests)
- **Observability**: Prometheus, Grafana, distributed tracing

**Perfect for demonstrating**: Full-stack backend engineering skills, cloud-native development, and production-ready code quality.

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          API Gateway / Load Balancer                │
└────────────────────────┬────────────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
┌────────▼────────┐ ┌───▼────────┐ ┌───▼──────────────┐
│  Banking        │ │ Analytics  │ │  Future Services │
│  Service        │ │ Service    │ │  (Payments, etc) │
│                 │ │            │ │                  │
│ • Accounts      │ │ • Real-time│ │                  │
│ • Transactions  │ │   Analytics│ │                  │
│ • Transfers     │ │ • User     │ │                  │
│                 │ │   Prefs    │ │                  │
└────────┬────────┘ └────┬───────┘ └──────────────────┘
         │               │
         │    ┌──────────▼──────────┐
         │    │   Kafka Event Bus   │
         └───►│  • Account Events   │
              │  • Transaction      │
              │    Events           │
              └─────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
┌────────▼────────┐ ┌───▼────────┐ ┌───▼──────────┐
│   PostgreSQL    │ │   Redis    │ │   MongoDB    │
│   (Banking DB)  │ │  (Cache)   │ │  (Analytics) │
└─────────────────┘ └────────────┘ └──────────────┘
```

### Architecture Highlights

- **Event-Driven**: Kafka for asynchronous communication
- **Polyglot Persistence**: PostgreSQL for transactions, Redis for caching, MongoDB for analytics
- **Schema-Based Multi-Tenancy**: Separate schemas for service isolation
- **CQRS Pattern**: Separate read/write models for analytics
- **Cloud-Native**: Designed for Kubernetes deployment on AWS EKS

---

## ✨ Key Features

### Banking Service
- ✅ **Account Management**: Create, update, deactivate/reactivate accounts
- ✅ **Transaction Processing**: Deposits, withdrawals, transfers
- ✅ **Balance Tracking**: Real-time balance calculations
- ✅ **Event Publishing**: Kafka events for all account/transaction changes
- ✅ **RESTful API**: OpenAPI/Swagger documentation
- ✅ **Data Validation**: Comprehensive input validation
- ✅ **Error Handling**: Global exception handling with proper HTTP status codes

### Analytics Service
- ✅ **Real-Time Analytics**: Compute spending patterns and trends
- ✅ **User Preferences**: Persistent user settings with caching
- ✅ **Redis Caching**: Cache-aside pattern with configurable TTL
- ✅ **MongoDB Storage**: Document-based analytics storage
- ✅ **Event Consumption**: Kafka consumer for transaction events

### Infrastructure
- ✅ **AWS EKS**: Production-ready Kubernetes cluster
- ✅ **Terraform IaC**: Complete infrastructure as code
- ✅ **Multi-Environment**: Dev, staging, production configurations
- ✅ **Helm Charts**: Kubernetes deployment automation
- ✅ **Monitoring**: Prometheus + Grafana stack
- ✅ **Secrets Management**: AWS Secrets Manager integration

### DevOps & CI/CD
- ✅ **GitHub Actions**: Automated build, test, deploy pipelines
- ✅ **Docker**: Multi-stage builds for optimized images
- ✅ **Multi-Arch Support**: AMD64 and ARM64 builds
- ✅ **Test Automation**: 113 automated tests (66 unit + 47 integration)
- ✅ **Code Quality**: Automated linting and security scanning

---

## 🛠 Technology Stack

### Backend
- **Java 21** - Latest LTS with modern language features
- **Spring Boot 3.3.4** - Enterprise application framework
- **Spring Data JPA** - Database access with Hibernate
- **Spring Data Redis** - Caching layer
- **Spring Data MongoDB** - Document storage
- **Spring Kafka** - Event streaming
- **Flyway** - Database migrations

### Databases
- **PostgreSQL 15** - Primary transactional database
- **Redis 7** - High-performance caching
- **MongoDB** - Analytics and document storage

### Messaging
- **Apache Kafka** - Event streaming platform
- **Spring Cloud Stream** - Message-driven microservices

### Infrastructure
- **AWS EKS** - Managed Kubernetes service
- **Terraform** - Infrastructure as Code
- **Helm** - Kubernetes package manager
- **Docker** - Containerization
- **LocalStack** - Local AWS cloud stack

### Observability
- **Prometheus** - Metrics collection
- **Grafana** - Visualization and dashboards
- **Micrometer** - Application metrics
- **Spring Boot Actuator** - Health checks and monitoring

### Testing
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **Testcontainers** - Integration testing with real dependencies
- **AssertJ** - Fluent assertions
- **Spring Boot Test** - Integration test support

### Development Tools
- **Maven** - Build automation
- **Lombok** - Reduce boilerplate code
- **OpenAPI/Swagger** - API documentation
- **Git** - Version control

---

## 🚀 Services

### 1. Banking Microservice
**Path**: `spring-boot-microservices-demo/`

Core banking service handling accounts and transactions.

**Key Endpoints**:
- `POST /api/accounts` - Create account
- `GET /api/accounts/{id}` - Get account details
- `POST /api/transactions` - Create transaction
- `POST /api/transactions/transfer` - Transfer funds
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Metrics

**Features**:
- Full CRUD operations for accounts
- Transaction processing with validation
- Kafka event publishing
- Redis caching
- Comprehensive test coverage (113 tests)

[📖 Full Documentation](./spring-boot-microservices-demo/README.md) | [🧪 Test Coverage](./spring-boot-microservices-demo/TEST-COVERAGE.md)

### 2. Account Analytics Service
**Path**: `account-analytics-service/`

Real-time analytics and user preferences service.

**Key Endpoints**:
- `POST /api/analytics/compute` - Compute account analytics
- `GET /api/analytics/{accountId}` - Get analytics
- `POST /api/preferences` - Save user preferences
- `GET /api/preferences/{userId}` - Get user preferences

**Features**:
- Real-time spending pattern analysis
- Redis caching with TTL
- MongoDB document storage
- Kafka event consumption
- User preference management

[📖 Full Documentation](./account-analytics-service/README.md)

### 3. Shared DTOs
**Path**: `shared-dtos/`

Common data transfer objects shared across services.

**Includes**:
- Account data models
- Transaction data models
- Event message formats

---

## 🏃 Getting Started

### Prerequisites

- **Java 21** ([SDKMAN](https://sdkman.io/) recommended)
- **Maven 3.9+**
- **Docker & Docker Compose**
- **AWS CLI** (for cloud deployment)
- **Terraform** (for infrastructure)
- **kubectl** (for Kubernetes)

### Quick Start (Local Development)

#### 1. Clone the Repository
```bash
git clone https://github.com/Tmejs/portfolio.git
cd portfolio
```

#### 2. Start Infrastructure Services
```bash
# Start PostgreSQL, Redis, Kafka using Docker Compose
docker-compose up -d
```

#### 3. Build All Services
```bash
# Build shared DTOs first
cd shared-dtos
mvn clean install

# Build banking service
cd ../spring-boot-microservices-demo
mvn clean install

# Build analytics service
cd ../account-analytics-service
mvn clean install
```

#### 4. Run Banking Service
```bash
cd spring-boot-microservices-demo
mvn spring-boot:run
```

The service will start on `http://localhost:8080`

#### 5. Run Analytics Service
```bash
cd account-analytics-service
mvn spring-boot:run
```

The service will start on `http://localhost:8081`

### Access Points

- **Banking API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus

---

## 🧪 Testing

### Test Coverage: 113 Tests ✅

- **Unit Tests**: 66 tests (~5 seconds)
- **Integration Tests**: 47 tests (~28 seconds)
- **Total Coverage**: Service, Controller, and Repository layers

### Run All Tests
```bash
cd spring-boot-microservices-demo
mvn verify
```

### Run Only Unit Tests
```bash
mvn test
```

### Run Only Integration Tests
```bash
mvn verify -DskipUnitTests=true
```

### Test Reports
- Unit test reports: `target/surefire-reports/`
- Integration test reports: `target/failsafe-reports/`

[📊 Detailed Test Coverage Report](./spring-boot-microservices-demo/TEST-COVERAGE.md)

---

## ☁️ Deployment

### AWS EKS Deployment

#### 1. Deploy Infrastructure
```bash
cd aws-infrastructure-terraform

# Initialize Terraform
terraform init

# Plan deployment
terraform plan -var-file="dev.tfvars"

# Deploy infrastructure
terraform apply -var-file="dev.tfvars"
```

#### 2. Configure kubectl
```bash
aws eks update-kubeconfig --region us-west-2 --name portfolio-microservices-dev-eks
kubectl get nodes
```

#### 3. Deploy Services
```bash
cd ../helm-charts/banking-microservice

helm install banking-microservice . \
  -f values-dev.yaml \
  --set-string database.host=<RDS_ENDPOINT> \
  --set-string redis.host=<REDIS_ENDPOINT>
```

### Local Development with LocalStack
```bash
cd aws-infrastructure-terraform

# Start LocalStack
docker-compose -f docker-compose.localstack.yml up -d

# Deploy to LocalStack
terraform apply -var-file="localstack.tfvars"
```

[📖 Infrastructure Documentation](./README-INFRASTRUCTURE.md)

---

## 📁 Project Structure

```
portfolio/
├── spring-boot-microservices-demo/    # Banking microservice
│   ├── src/
│   │   ├── main/java/                 # Application code
│   │   │   ├── controller/            # REST controllers
│   │   │   ├── service/               # Business logic
│   │   │   ├── repository/            # Data access
│   │   │   ├── entity/                # JPA entities
│   │   │   └── dto/                   # Data transfer objects
│   │   ├── test/java/                 # Test code
│   │   │   ├── controller/            # Controller tests
│   │   │   ├── service/               # Service tests
│   │   │   └── repository/            # Repository tests
│   │   └── resources/
│   │       ├── application.yml        # Main configuration
│   │       └── db/migration/          # Flyway migrations
│   ├── TEST-COVERAGE.md               # Test documentation
│   └── README.md                      # Service documentation
│
├── account-analytics-service/         # Analytics microservice
│   ├── src/
│   │   ├── main/java/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── model/
│   │   └── test/java/
│   └── README.md
│
├── shared-dtos/                       # Shared data models
│   └── src/main/java/
│       └── com/portfolio/shared/dto/
│
├── aws-infrastructure-terraform/      # Infrastructure as Code
│   ├── modules/                       # Terraform modules
│   │   ├── vpc/
│   │   ├── eks/
│   │   ├── rds/
│   │   └── redis/
│   ├── dev.tfvars                     # Dev environment
│   ├── prod.tfvars                    # Prod environment
│   └── README.md
│
├── helm-charts/                       # Kubernetes deployments
│   └── banking-microservice/
│       ├── values-dev.yaml
│       └── values-prod.yaml
│
├── .github/                           # CI/CD pipelines
│   └── workflows/
│       ├── ci-cd.yml                  # Main pipeline
│       ├── pr-validation.yml          # PR checks
│       └── account-analytics-service.yml
│
└── README.md                          # This file
```

---

## 📚 Documentation

### Service Documentation
- [Banking Service README](./spring-boot-microservices-demo/README.md)
- [Analytics Service README](./account-analytics-service/README.md)
- [Infrastructure README](./README-INFRASTRUCTURE.md)
- [Test Coverage Report](./spring-boot-microservices-demo/TEST-COVERAGE.md)

### API Documentation
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/v3/api-docs`

### Architecture Documentation
- [WARP.md](./WARP.md) - Development workflow guide
- [GitHub Workflows](./.github/README.md) - CI/CD documentation

---

## 🎓 Skills Demonstrated

This project showcases expertise in:

### Backend Development
- ✅ Modern Java 21 features (records, pattern matching, virtual threads)
- ✅ Spring Boot 3.x ecosystem
- ✅ RESTful API design
- ✅ Database design and optimization
- ✅ Caching strategies
- ✅ Event-driven architecture

### Microservices
- ✅ Service decomposition
- ✅ Inter-service communication
- ✅ Event sourcing patterns
- ✅ CQRS implementation
- ✅ API gateway patterns

### Data Management
- ✅ Relational databases (PostgreSQL)
- ✅ NoSQL databases (MongoDB)
- ✅ Caching (Redis)
- ✅ Database migrations (Flyway)
- ✅ Transaction management

### DevOps & Cloud
- ✅ Infrastructure as Code (Terraform)
- ✅ Container orchestration (Kubernetes)
- ✅ CI/CD pipelines (GitHub Actions)
- ✅ Cloud platforms (AWS EKS)
- ✅ Monitoring and observability

### Testing
- ✅ Unit testing (JUnit, Mockito)
- ✅ Integration testing (Testcontainers)
- ✅ Test-driven development
- ✅ High test coverage (113 tests)

### Software Engineering
- ✅ Clean code principles
- ✅ SOLID principles
- ✅ Design patterns
- ✅ Git workflow
- ✅ Documentation

---

## 🤝 Contributing

This is a portfolio project, but suggestions and feedback are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Mateusz Rzad**

- GitHub: [@Tmejs](https://github.com/Tmejs)
- LinkedIn: [Your LinkedIn Profile]
- Email: [Your Email]

---

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Testcontainers for making integration testing easier
- AWS for cloud infrastructure
- Open source community for amazing tools and libraries

---

<div align="center">

**⭐ If you find this project helpful, please consider giving it a star!**

Built with ❤️ using Spring Boot, Java 21, and modern cloud technologies

</div>
