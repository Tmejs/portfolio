package com.portfolio.demo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for repository integration tests with proper session management.
 * Provides PostgreSQL container and transactional test support.
 * 
 * Best practices implemented:
 * - Uses method-level @Transactional for tests that need it
 * - Enables lazy loading for test scenarios
 * - Proper session management for integration tests
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractRepositoryTest {

    @Container
    protected static final PostgreSQLContainer<?> postgresql = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("banking_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

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
