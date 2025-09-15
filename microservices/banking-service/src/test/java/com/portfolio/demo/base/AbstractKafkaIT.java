package com.portfolio.demo.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Abstract base class for Kafka integration tests with Testcontainers
 * 
 * This class sets up PostgreSQL and Kafka containers for integration testing
 * and configures the necessary properties for Spring Boot to connect to them.
 */
@SpringBootTest
@Testcontainers
@DirtiesContext
public abstract class AbstractKafkaIT {

    @Container
    protected static final PostgreSQLContainer<?> postgresql = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("banking_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    @Container
    protected static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure database properties
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
        // Disable Redis but configure Kafka
        registry.add("spring.redis.host", () -> "disabled");
        
        // Configure Kafka properties
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        
        // Configure Kafka consumer properties for tests
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.key-deserializer", () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("spring.kafka.consumer.value-deserializer", () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "com.portfolio.demo.dto.message");
        
        // Configure Kafka producer properties for tests
        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        
        // Configure Kafka topics for tests
        registry.add("banking.messaging.kafka.topics.account-created", () -> "account-created");
        registry.add("banking.messaging.kafka.topics.transaction-created", () -> "transaction-created");
        registry.add("banking.messaging.kafka.topics.transaction-transfer", () -> "transaction-transfer");
        registry.add("banking.messaging.kafka.topics.partitions", () -> "1");
        registry.add("banking.messaging.kafka.topics.replication-factor", () -> "1");
    }

    @org.junit.jupiter.api.Test
    void containersAreRunning() {
        // Verify that containers are running
        org.assertj.core.api.Assertions.assertThat(postgresql.isRunning()).isTrue();
        org.assertj.core.api.Assertions.assertThat(kafka.isRunning()).isTrue();
    }
}
