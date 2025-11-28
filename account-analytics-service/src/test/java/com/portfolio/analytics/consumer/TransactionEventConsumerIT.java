package com.portfolio.analytics.consumer;

import com.portfolio.analytics.BaseAnalyticsIT;
import com.portfolio.analytics.event.TransactionEvent;
import com.portfolio.analytics.model.AccountAnalytics;
import com.portfolio.analytics.service.AccountAnalyticsService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(
    partitions = 1,
    topics = {"transaction-events"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9097", "port=9097"}
)
@DirtiesContext
@DisplayName("Transaction Event Consumer Integration Tests")
class TransactionEventConsumerIT extends BaseAnalyticsIT {

    @Autowired
    private TransactionEventConsumer transactionEventConsumer;

    @MockBean
    private AccountAnalyticsService accountAnalyticsService;

    private KafkaProducer<String, TransactionEvent> producer;

    @BeforeEach
    void setUp() {
        // Configure Kafka producer for testing
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9097");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        producer = new KafkaProducer<>(producerProps);
        
        // Reset mocks
        reset(accountAnalyticsService);
    }

    @Test
    @DisplayName("Should process transaction created event and update analytics incrementally")
    void shouldProcessTransactionCreatedEventWithIncrementalUpdate() {
        // Given
        String accountId = "account-123";
        
        AccountAnalytics existingAnalytics = AccountAnalytics.builder()
            .accountId(accountId)
            .totalBalance(BigDecimal.valueOf(1000))
            .totalIncome(BigDecimal.valueOf(1500))
            .totalExpenses(BigDecimal.valueOf(500))
            .transactionCount(10L)
            .depositCount(6L)
            .withdrawalCount(4L)
            .largestDeposit(BigDecimal.valueOf(200))
            .largestWithdrawal(BigDecimal.valueOf(150))
            .build();

        when(accountAnalyticsService.getAccountAnalytics(accountId))
            .thenReturn(Optional.of(existingAnalytics));

        TransactionEvent event = TransactionEvent.builder()
            .transactionId("tx-123")
            .accountId(accountId)
            .amount(BigDecimal.valueOf(250.00))
            .transactionType("DEPOSIT")
            .category("SALARY")
            .timestamp(LocalDateTime.now())
            .eventType("CREATED")
            .source("banking-service")
            .eventTimestamp(LocalDateTime.now())
            .build();

        // When
        producer.send(new ProducerRecord<>("transaction-events", accountId, event));

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(accountAnalyticsService).getAccountAnalytics(accountId);
                verify(accountAnalyticsService).saveAccountAnalytics(argThat(analytics -> 
                    analytics.getAccountId().equals(accountId) &&
                    analytics.getTotalBalance().equals(BigDecimal.valueOf(1250)) &&
                    analytics.getTotalIncome().equals(BigDecimal.valueOf(1750)) &&
                    analytics.getTransactionCount().equals(11L) &&
                    analytics.getDepositCount().equals(7L) &&
                    analytics.getLargestDeposit().equals(BigDecimal.valueOf(250))
                ));
            });
    }

    @Test
    @DisplayName("Should process transaction created event for account without existing analytics")
    void shouldProcessTransactionCreatedEventWithoutExistingAnalytics() {
        // Given
        String accountId = "account-456";
        
        when(accountAnalyticsService.getAccountAnalytics(accountId))
            .thenReturn(Optional.empty());

        TransactionEvent event = TransactionEvent.builder()
            .transactionId("tx-456")
            .accountId(accountId)
            .amount(BigDecimal.valueOf(100.00))
            .transactionType("DEPOSIT")
            .category("TRANSFER")
            .timestamp(LocalDateTime.now())
            .eventType("CREATED")
            .source("banking-service")
            .eventTimestamp(LocalDateTime.now())
            .build();

        // When
        producer.send(new ProducerRecord<>("transaction-events", accountId, event));

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(accountAnalyticsService).getAccountAnalytics(accountId);
                verify(accountAnalyticsService).invalidateAnalytics(accountId);
                verify(accountAnalyticsService, never()).saveAccountAnalytics(any());
            });
    }

    @Test
    @DisplayName("Should process transaction updated event and invalidate cache")
    void shouldProcessTransactionUpdatedEvent() {
        // Given
        String accountId = "account-789";
        
        TransactionEvent event = TransactionEvent.builder()
            .transactionId("tx-789")
            .accountId(accountId)
            .amount(BigDecimal.valueOf(75.00))
            .transactionType("WITHDRAWAL")
            .category("ATM")
            .timestamp(LocalDateTime.now())
            .eventType("UPDATED")
            .source("banking-service")
            .eventTimestamp(LocalDateTime.now())
            .build();

        // When
        producer.send(new ProducerRecord<>("transaction-events", accountId, event));

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(accountAnalyticsService).invalidateAnalytics(accountId);
                verify(accountAnalyticsService, never()).getAccountAnalytics(any());
                verify(accountAnalyticsService, never()).saveAccountAnalytics(any());
            });
    }

    @Test
    @DisplayName("Should process transaction deleted event and invalidate cache")
    void shouldProcessTransactionDeletedEvent() {
        // Given
        String accountId = "account-321";
        
        TransactionEvent event = TransactionEvent.builder()
            .transactionId("tx-321")
            .accountId(accountId)
            .amount(BigDecimal.valueOf(50.00))
            .transactionType("PURCHASE")
            .category("GROCERY")
            .timestamp(LocalDateTime.now())
            .eventType("DELETED")
            .source("banking-service")
            .eventTimestamp(LocalDateTime.now())
            .build();

        // When
        producer.send(new ProducerRecord<>("transaction-events", accountId, event));

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(accountAnalyticsService).invalidateAnalytics(accountId);
                verify(accountAnalyticsService, never()).getAccountAnalytics(any());
                verify(accountAnalyticsService, never()).saveAccountAnalytics(any());
            });
    }

    @Test
    @DisplayName("Should handle withdrawal transaction correctly")
    void shouldHandleWithdrawalTransactionCorrectly() {
        // Given
        String accountId = "account-withdrawal";
        
        AccountAnalytics existingAnalytics = AccountAnalytics.builder()
            .accountId(accountId)
            .totalBalance(BigDecimal.valueOf(500))
            .totalIncome(BigDecimal.valueOf(1000))
            .totalExpenses(BigDecimal.valueOf(500))
            .transactionCount(8L)
            .depositCount(4L)
            .withdrawalCount(4L)
            .largestDeposit(BigDecimal.valueOf(300))
            .largestWithdrawal(BigDecimal.valueOf(200))
            .build();

        when(accountAnalyticsService.getAccountAnalytics(accountId))
            .thenReturn(Optional.of(existingAnalytics));

        TransactionEvent event = TransactionEvent.builder()
            .transactionId("tx-withdrawal")
            .accountId(accountId)
            .amount(BigDecimal.valueOf(-150.00)) // Negative for withdrawal
            .transactionType("WITHDRAWAL")
            .category("ATM")
            .timestamp(LocalDateTime.now())
            .eventType("CREATED")
            .source("banking-service")
            .eventTimestamp(LocalDateTime.now())
            .build();

        // When
        producer.send(new ProducerRecord<>("transaction-events", accountId, event));

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(accountAnalyticsService).getAccountAnalytics(accountId);
                verify(accountAnalyticsService).saveAccountAnalytics(argThat(analytics -> 
                    analytics.getAccountId().equals(accountId) &&
                    analytics.getTotalBalance().equals(BigDecimal.valueOf(350)) && // 500 + (-150)
                    analytics.getTotalExpenses().equals(BigDecimal.valueOf(650)) && // 500 + 150
                    analytics.getTransactionCount().equals(9L) &&
                    analytics.getWithdrawalCount().equals(5L) &&
                    analytics.getLargestWithdrawal().equals(BigDecimal.valueOf(200)) // Still 200, not updated
                ));
            });
    }

    @Test
    @DisplayName("Should skip invalid transaction events")
    void shouldSkipInvalidTransactionEvents() {
        // Given
        TransactionEvent invalidEvent = TransactionEvent.builder()
            .transactionId("tx-invalid")
            // Missing accountId
            .amount(BigDecimal.valueOf(100.00))
            .eventType("CREATED")
            .build();

        // When
        producer.send(new ProducerRecord<>("transaction-events", "invalid-key", invalidEvent));

        // Then - wait a bit and verify no interactions
        await().pollDelay(2, TimeUnit.SECONDS)
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verifyNoInteractions(accountAnalyticsService);
            });
    }

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    void shouldHandleServiceExceptionsGracefully() {
        // Given
        String accountId = "account-error";
        
        when(accountAnalyticsService.getAccountAnalytics(accountId))
            .thenThrow(new RuntimeException("Service error"));

        TransactionEvent event = TransactionEvent.builder()
            .transactionId("tx-error")
            .accountId(accountId)
            .amount(BigDecimal.valueOf(100.00))
            .transactionType("DEPOSIT")
            .category("TRANSFER")
            .timestamp(LocalDateTime.now())
            .eventType("CREATED")
            .source("banking-service")
            .eventTimestamp(LocalDateTime.now())
            .build();

        // When
        producer.send(new ProducerRecord<>("transaction-events", accountId, event));

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(accountAnalyticsService).getAccountAnalytics(accountId);
                verify(accountAnalyticsService).invalidateAnalytics(accountId);
                verify(accountAnalyticsService, never()).saveAccountAnalytics(any());
            });
    }
}
