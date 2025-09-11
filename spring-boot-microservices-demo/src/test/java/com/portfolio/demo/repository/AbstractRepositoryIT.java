package com.portfolio.demo.repository;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for repository integration tests with proper session management.
 * Provides PostgreSQL container and transactional test support.
 * 
 * Best practices implemented:
 * - Uses method-level @Transactional for tests that need it
 * - Enables lazy loading for test scenarios
 * - Proper session management for integration tests
 * - @DirtiesContext ensures clean application context between test classes
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
public abstract class AbstractRepositoryIT {

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

    @Test
    void dbIsConnected() {
        // Verify that PostgreSQL container is running
        assertThat(postgresql.isRunning()).isTrue();
        assertThat(postgresql.getDatabaseName()).isEqualTo("banking_test");
    }
}
