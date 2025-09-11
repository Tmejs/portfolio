package com.portfolio.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot Microservices Demo Application
 * 
 * This application demonstrates modern Java 24 features including:
 * - Spring Boot 3.x with latest features
 * - PostgreSQL integration with JPA
 * - Redis for caching
 * - Kafka for async messaging
 * - Modern Java 24 syntax and features
 */
@SpringBootApplication
@EnableAsync
@EnableKafka
public class MicroservicesDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroservicesDemoApplication.class, args);
    }
}
