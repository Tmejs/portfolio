package com.portfolio.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify Spring Boot application context loads
 * with real PostgreSQL database using Testcontainers with improved session management
 */
@SpringBootTest
class BankingApplicationTest extends AbstractRepositoryTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Verify that the application context loads successfully
        assertThat(applicationContext).isNotNull();
        
        // Verify that PostgreSQL container is running
        assertThat(postgresql.isRunning()).isTrue();
        assertThat(postgresql.getDatabaseName()).isEqualTo("banking_test");
    }

    @Test
    void verifyTestcontainerConnection() {
        // Verify we can connect to the Testcontainer PostgreSQL instance
        String jdbcUrl = postgresql.getJdbcUrl();
        assertThat(jdbcUrl).contains("postgresql://");
        assertThat(jdbcUrl).contains("banking_test");
        
        // Verify credentials
        assertThat(postgresql.getUsername()).isEqualTo("test_user");
        assertThat(postgresql.getPassword()).isEqualTo("test_password");
    }
}
