package com.portfolio.demo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using Testcontainers
 * Provides PostgreSQL container for all integration tests
 * Designed for Java 24 with banking microservices
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> postgresql = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("banking_test")
            .withUsername("test_user")
            .withPassword("test_password");
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
        
        // Disable Redis and Kafka for integration tests
        registry.add("spring.redis.host", () -> "disabled");
        registry.add("spring.kafka.bootstrap-servers", () -> "disabled");
    }
}
