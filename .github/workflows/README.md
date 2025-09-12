# Portfolio Services CI/CD Workflows

This directory contains GitHub Actions workflows for the portfolio microservices architecture with intelligent path-based triggering.

## 🏗️ Workflow Architecture

### Multi-Service Strategy
The workflows are designed to handle multiple microservices efficiently:
- **Banking Service**: `spring-boot-microservices-demo/`
- **Analytics Service**: `account-analytics-service/`

### Path-Based Triggering
Each workflow only runs when relevant code changes are detected:

```yaml
# Example: Only run when analytics service changes
paths:
  - 'account-analytics-service/**'
  - '.github/workflows/account-analytics-service.yml'
```

## 📁 Workflow Files

### 1. `ci-cd.yml` - Main Pipeline
- **Purpose**: Handles banking service CI/CD
- **Triggers**: Changes to `spring-boot-microservices-demo/`
- **Jobs**: Build, Test, Package, Docker, Security Scan
- **Features**: 
  - Multi-service change detection
  - Service-specific artifacts and caching
  - Enhanced reporting with GitHub Step Summaries

### 2. `account-analytics-service.yml` - Analytics Pipeline
- **Purpose**: Dedicated CI/CD for analytics service
- **Triggers**: Changes to `account-analytics-service/`
- **Jobs**: Build, Test (Matrix), Code Quality, Package, Docker
- **Features**:
  - Testcontainers integration for Redis testing
  - SpotBugs and OWASP dependency scanning
  - Multi-platform Docker builds
  - Comprehensive test reporting

### 3. `pr-validation.yml` - PR Validation
- **Purpose**: Quick validation for pull requests
- **Triggers**: All PRs to main/develop
- **Jobs**: Service-specific validation based on changes
- **Features**:
  - Intelligent PR comments with per-service results
  - Quick smoke tests for faster feedback
  - Service isolation prevents unnecessary runs

## 🔧 Change Detection Logic

Uses `dorny/paths-filter@v2` for intelligent change detection:

```yaml
filters: |
  banking-service:
    - 'spring-boot-microservices-demo/**'
    - '.github/workflows/ci-cd.yml'
  analytics-service:
    - 'account-analytics-service/**'
    - '.github/workflows/account-analytics-service.yml'
```

## 🎯 Key Benefits

### ⚡ Efficiency
- **Reduced CI minutes**: Only run jobs for changed services
- **Faster feedback**: Parallel execution for independent services
- **Smart caching**: Service-specific Maven cache keys

### 🧪 Testing
- **Testcontainers**: Real Redis/MongoDB integration testing
- **Matrix strategy**: Separate unit and integration test jobs
- **Comprehensive reporting**: Test summaries in GitHub UI

### 📦 Artifacts
- **Service isolation**: Separate artifacts per service
- **Docker images**: Multi-platform builds for production
- **Code quality**: Security scanning and static analysis

### 🔍 Monitoring
- **GitHub Step Summaries**: Rich reporting in GitHub UI
- **PR comments**: Service-specific validation results
- **Job dependencies**: Clear workflow visualization

## 🚀 Usage Examples

### Banking Service Changes
```bash
# Only banking service workflow runs
git add spring-boot-microservices-demo/src/main/java/com/portfolio/demo/service/AccountService.java
git commit -m "feat: add new account validation"
```

### Analytics Service Changes
```bash
# Only analytics service workflow runs
git add account-analytics-service/src/main/java/com/portfolio/analytics/service/CacheService.java
git commit -m "feat: improve cache warming strategy"
```

### Both Services
```bash
# Both workflows run in parallel
git add spring-boot-microservices-demo/src/main/java/com/portfolio/demo/controller/AccountController.java
git add account-analytics-service/src/main/java/com/portfolio/analytics/service/AnalyticsService.java
git commit -m "feat: add new analytics endpoint integration"
```

## 📊 Workflow Matrices

### Analytics Service Test Matrix
```yaml
strategy:
  matrix:
    test-type: [unit, integration]
  fail-fast: false
```

### Docker Build Platforms
```yaml
platforms: linux/amd64,linux/arm64
```

## 🔐 Security & Quality

### OWASP Dependency Scanning
- CVE threshold: 8.0 for analytics, 7.0 for banking
- HTML reports uploaded as artifacts

### SpotBugs Analysis
- Static code analysis for Java services
- XML reports for integration with other tools

### Docker Security
- Non-root users in containers
- Health checks with proper timeouts
- Multi-stage builds for minimal attack surface

## 🏷️ Branch Strategy

### Main Branches
- **`main`**: Production deployments, full pipeline
- **`develop`**: Integration testing, full pipeline
- **`caching-service`**: Analytics service preview builds

### Docker Tags
- `latest`: Main branch builds
- `sha-<commit>`: All branch builds
- `caching-preview`: Caching service branch builds

## 📝 PR Workflow Example

1. **Developer creates PR** with analytics service changes
2. **Change detection** identifies only analytics changes
3. **PR validation** runs only analytics validation job
4. **PR comment** shows analytics-specific results
5. **On merge**, full analytics pipeline runs with Docker build

## 🔧 Configuration

### Environment Variables
```yaml
env:
  JAVA_VERSION: '21'
  MAVEN_OPTS: '-Xmx1024m'
  SPRING_PROFILES_ACTIVE: test
  SERVICE_NAME: account-analytics-service
```

### Secrets Required
- `DOCKER_HUB_USERNAME`: Docker Hub username
- `DOCKER_HUB_ACCESS_TOKEN`: Docker Hub access token

## 🎉 Future Enhancements

- [ ] Kubernetes deployment workflows
- [ ] Integration testing between services
- [ ] Performance testing with load generation
- [ ] Automatic dependency updates with Dependabot integration
- [ ] SonarQube integration for code quality metrics
