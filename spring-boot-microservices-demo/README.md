# Spring Boot Microservices Demo

A comprehensive Spring Boot application demonstrating modern Java 24 features, enterprise-grade architecture patterns, and cloud-native technologies.

## 🚀 Features

- **Modern Java 24**: Leverages the latest Java features including pattern matching, record classes, and virtual threads
- **Spring Boot 3.x**: Latest Spring Boot with enhanced performance and security
- **Microservices Architecture**: Scalable, distributed system design
- **Database Integration**: PostgreSQL with JPA/Hibernate and Flyway migrations
- **Caching**: Redis for high-performance caching
- **Message Queuing**: Apache Kafka for async communication
- **Observability**: Prometheus metrics and health checks
- **API Documentation**: OpenAPI/Swagger integration
- **Testing**: Comprehensive test coverage with TestContainers

## 🛠 Tech Stack

- **Java 24** with preview features enabled
- **Spring Boot 3.3.4**
- **Maven** for build management
- **PostgreSQL** as primary database
- **Redis** for caching
- **Apache Kafka** for messaging
- **Docker** for containerization
- **TestContainers** for integration testing

## 🏗 Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Layer     │    │  Service Layer  │    │   Data Layer    │
│                 │    │                 │    │                 │
│ REST Controllers│────│ Business Logic  │────│ JPA Repositories│
│ Exception       │    │ Async Processing│    │ PostgreSQL      │
│ Handlers        │    │ Event Publishing│    │ Redis Cache     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │ Messaging Layer │
                    │                 │
                    │ Kafka Producers │
                    │ Kafka Consumers │
                    └─────────────────┘
```

## 🏃‍♂️ Quick Start

### Prerequisites

- Java 24 (use SDKMAN: `sdk install java 24-tem`)
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL (or use Docker)
- Redis (or use Docker)
- Apache Kafka (or use Docker)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd spring-boot-microservices-demo
   ```

2. **Set up Java 24**
   ```bash
   sdk use java 24-tem
   java -version
   ```

3. **Build the project**
   ```bash
   mvn clean compile
   ```

4. **Run with Docker Compose** (recommended)
   ```bash
   docker-compose up -d
   mvn spring-boot:run
   ```

5. **Or run individual services**
   ```bash
   # PostgreSQL
   docker run -d --name postgres -e POSTGRES_DB=microservices -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=password -p 5432:5432 postgres:15
   
   # Redis
   docker run -d --name redis -p 6379:6379 redis:7-alpine
   
   # Kafka (with Zookeeper)
   docker-compose -f docker-compose.kafka.yml up -d
   
   # Run the application
   mvn spring-boot:run
   ```

## 📊 API Documentation

Once the application is running, you can access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run integration tests with TestContainers
mvn verify

# Run specific test class
mvn test -Dtest=UserServiceTest
```

## 🔧 Configuration

Key configuration files:
- `application.yml` - Main application configuration
- `application-dev.yml` - Development environment
- `application-prod.yml` - Production environment

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=microservices
DB_USERNAME=admin
DB_PASSWORD=password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## 🚀 Modern Java 24 Features Demonstrated

- **Pattern Matching**: Enhanced switch expressions and pattern matching
- **Record Classes**: Immutable data carriers
- **Virtual Threads**: Lightweight concurrency with Project Loom
- **Text Blocks**: Multi-line string literals
- **Sealed Classes**: Restricted class hierarchies
- **Foreign Function & Memory API**: Native code integration

## 📈 Performance & Monitoring

- **Micrometer**: Application metrics
- **Prometheus**: Metrics collection
- **Spring Boot Actuator**: Health checks and operational endpoints
- **Async Processing**: Non-blocking operations with CompletableFuture

## 🔒 Security

- **Spring Security**: Authentication and authorization
- **JWT Tokens**: Stateless authentication
- **CORS Configuration**: Cross-origin resource sharing
- **Security Headers**: XSS protection, CSRF protection

## 📦 Deployment

This application is designed to run on Kubernetes (EKS) with the accompanying Terraform infrastructure. See the `aws-infrastructure-terraform` repository for deployment scripts.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 About

This project demonstrates enterprise Java development skills including:
- Modern Java language features
- Spring ecosystem expertise
- Microservices architecture patterns
- Database design and optimization
- Message-driven architecture
- DevOps and cloud-native development
- Testing strategies and automation
