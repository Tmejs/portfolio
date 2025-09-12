# Account Analytics Service

A Spring Boot 3.3.4 microservice that provides real-time account analytics with Redis caching capabilities.

## Features

### Account Analytics
- **Real-time computation** of account metrics from transaction data
- **Spending pattern analysis** (Conservative, Moderate, Aggressive, Inactive)
- **Monthly/Daily breakdowns** with income and expense tracking
- **Volatility scoring** based on transaction patterns
- **Primary category detection** from transaction data

### Redis Caching
- **Cache-aside pattern** implementation for analytics and user preferences
- **Configurable TTL** for different data types (analytics: 15min, preferences: 2h)
- **Cache invalidation** strategies with manual and automatic eviction
- **Cache warm-up** capabilities for performance optimization

### User Preferences
- **Persistent storage** of user settings (theme, language, currency, timezone)
- **Custom settings** support with flexible metadata
- **Cached retrieval** for fast access to frequently used preferences

## Technology Stack

- **Spring Boot 3.3.4** with Java 21
- **Spring Data Redis** for caching layer
- **Spring Cache Abstraction** for declarative caching
- **Testcontainers** for integration testing
- **Lombok** for reducing boilerplate code
- **Jackson** for JSON serialization with Java time support

## Quick Start

### Prerequisites
- Java 21
- Docker (for Redis and testing)
- Maven 3.6+

### Running the Service
```bash
# Build the service
mvn clean compile

# Run with development profile
mvn spring-boot:run

# Run tests with Testcontainers
mvn test
```

### Configuration
The service requires Redis connection configuration:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
```

## API Usage

### Account Analytics
```java
// Compute analytics from transaction data
List<TransactionData> transactions = getTransactionData(accountId);
AccountAnalytics analytics = accountAnalyticsService.computeAnalytics(accountId, transactions);

// Cache the results
AccountAnalytics cached = accountAnalyticsService.saveAccountAnalytics(analytics);

// Retrieve cached analytics
Optional<AccountAnalytics> result = accountAnalyticsService.getAccountAnalytics(accountId);
```

### User Preferences
```java
// Save user preferences (cached automatically)
UserPreferences prefs = UserPreferences.builder()
    .userId("user123")
    .theme("dark")
    .language("en")
    .currency("USD")
    .build();
UserPreferences saved = userPreferencesService.saveUserPreferences(prefs);

// Retrieve from cache
Optional<UserPreferences> cached = userPreferencesService.getUserPreferences("user123");
```

## Testing

The service includes comprehensive integration tests using Testcontainers:

```bash
# Run Redis integration tests
mvn test -Dtest=RedisIT

# Run analytics integration tests
mvn test -Dtest=AccountAnalyticsIT

# Run all tests
mvn test
```

### Test Coverage
- ✅ Redis connection and serialization
- ✅ User preferences CRUD with caching
- ✅ Account analytics computation and caching
- ✅ Spending pattern determination
- ✅ Monthly/daily breakdowns
- ✅ Cache invalidation and warm-up
- ✅ Analytics existence and deletion

## Architecture

```
┌─────────────────┐    ┌──────────────┐    ┌─────────────┐
│   Controllers   │    │   Services   │    │   Redis     │
│                 │────│              │────│   Cache     │
│ • Analytics     │    │ • Analytics  │    │             │
│ • Preferences   │    │ • UserPrefs  │    └─────────────┘
└─────────────────┘    └──────────────┘           │
                              │                   │
                       ┌──────────────┐          │
                       │ Repositories │          │
                       │              │──────────┘
                       │ • Analytics  │
                       │ • UserPrefs  │
                       └──────────────┘
```

## Next Steps

- [ ] Add REST API controllers
- [ ] Integrate Kafka event consumers for real-time updates
- [ ] Add MongoDB for document storage
- [ ] Implement inter-service communication with banking service
- [ ] Add comprehensive observability (metrics, tracing)

## Configuration Examples

### Development
```yaml
spring:
  profiles:
    active: dev
  data:
    redis:
      host: localhost
      port: 6379
```

### Production
```yaml
spring:
  profiles:
    active: prod
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 20
          max-idle: 8
```
