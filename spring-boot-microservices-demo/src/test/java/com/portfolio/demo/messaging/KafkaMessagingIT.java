package com.portfolio.demo.messaging;

import com.portfolio.demo.base.AbstractKafkaIT;
import com.portfolio.demo.dto.message.AccountCreatedMessage;
import com.portfolio.demo.dto.message.TransactionCreatedMessage;
import com.portfolio.demo.dto.request.CreateAccountRequest;
import com.portfolio.demo.dto.request.CreateTransactionRequest;
import com.portfolio.demo.dto.response.AccountResponse;
import com.portfolio.demo.dto.response.TransactionResponse;
import com.portfolio.demo.enums.AccountType;
import com.portfolio.demo.enums.TransactionType;
import com.portfolio.demo.service.AccountService;
import com.portfolio.demo.service.TransactionService;
import com.portfolio.demo.service.messaging.MessageConsumerService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Kafka messaging functionality
 * Uses Testcontainers for Kafka and PostgreSQL
 */
class KafkaMessagingIT extends AbstractKafkaIT {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;
    
    // Mock the application's Kafka consumers to prevent them from consuming test messages
    @MockBean
    private MessageConsumerService messageConsumerService;
    
    // Stop Kafka listeners during tests to prevent competition with test consumers
    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Value("${banking.messaging.kafka.topics.account-created}")
    private String accountCreatedTopic;

    @Value("${banking.messaging.kafka.topics.transaction-created}")
    private String transactionCreatedTopic;

    private Consumer<String, AccountCreatedMessage> accountConsumer;
    private Consumer<String, TransactionCreatedMessage> transactionConsumer;

    @BeforeEach
    void setUp() {
        // Stop application Kafka listeners to prevent competition with test consumers
        kafkaListenerEndpointRegistry.stop();
        
        // Create Kafka consumers for test verification
        accountConsumer = createAccountConsumer();
        transactionConsumer = createTransactionConsumer();
        
        // Subscribe to topics
        accountConsumer.subscribe(Collections.singletonList(accountCreatedTopic));
        transactionConsumer.subscribe(Collections.singletonList(transactionCreatedTopic));
    }
    
    @AfterEach
    void tearDown() {
        // Close test consumers
        if (accountConsumer != null) {
            accountConsumer.close();
        }
        if (transactionConsumer != null) {
            transactionConsumer.close();
        }
        
        // Restart Kafka listeners for next test
        kafkaListenerEndpointRegistry.start();
    }

    @Test
    @Transactional
    void shouldPublishAccountCreatedMessageWhenAccountIsCreated() throws Exception {
        // Given
        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        // When
        AccountResponse response = accountService.createAccount(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCustomerName()).isEqualTo("John Doe");

        // Verify message was published to Kafka
        ConsumerRecords<String, AccountCreatedMessage> records = accountConsumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        
        ConsumerRecord<String, AccountCreatedMessage> record = records.iterator().next();
        AccountCreatedMessage message = record.value();
        
        assertThat(message.getAccountId()).isEqualTo(response.getId());
        assertThat(message.getAccountNumber()).isEqualTo(response.getAccountNumber());
        assertThat(message.getCustomerName()).isEqualTo("John Doe");
        assertThat(message.getCustomerEmail()).isEqualTo("john.doe@example.com");
        assertThat(message.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(message.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(message.getCurrency()).isEqualTo("USD");
        assertThat(message.getEventType()).isEqualTo("ACCOUNT_CREATED");
        assertThat(message.getEventVersion()).isEqualTo("1.0");
        assertThat(message.getEventTimestamp()).isNotNull();
    }

    @Test
    @Transactional
    void shouldPublishTransactionCreatedMessageWhenTransactionIsCreated() throws Exception {
        // Given - Create an account first
        CreateAccountRequest accountRequest = CreateAccountRequest.builder()
                .customerName("Jane Smith")
                .customerEmail("jane.smith@example.com")
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("500.00"))
                .currency("USD")
                .build();

        AccountResponse account = accountService.createAccount(accountRequest);
        
        // Consume the account creation message to clear it
        accountConsumer.poll(Duration.ofSeconds(10));

        CreateTransactionRequest transactionRequest = CreateTransactionRequest.builder()
                .accountId(account.getId())
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("250.00"))
                .description("Test deposit")
                .build();

        // When
        TransactionResponse transaction = transactionService.createTransaction(transactionRequest);

        // Then
        assertThat(transaction).isNotNull();
        assertThat(transaction.getAmount()).isEqualTo(new BigDecimal("250.00"));

        // Verify message was published to Kafka
        ConsumerRecords<String, TransactionCreatedMessage> records = transactionConsumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        
        ConsumerRecord<String, TransactionCreatedMessage> record = records.iterator().next();
        TransactionCreatedMessage message = record.value();
        
        assertThat(message.getTransactionId()).isEqualTo(transaction.getId());
        assertThat(message.getReferenceNumber()).isEqualTo(transaction.getReferenceNumber());
        assertThat(message.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(message.getAmount()).isEqualTo(new BigDecimal("250.00"));
        assertThat(message.getDescription()).isEqualTo("Test deposit");
        assertThat(message.getBalanceBefore()).isEqualTo(new BigDecimal("500.00"));
        assertThat(message.getBalanceAfter()).isEqualTo(new BigDecimal("750.00"));
        assertThat(message.getAccountId()).isEqualTo(account.getId());
        assertThat(message.getAccountNumber()).isEqualTo(account.getAccountNumber());
        assertThat(message.getCustomerName()).isEqualTo("Jane Smith");
        assertThat(message.getEventType()).isEqualTo("TRANSACTION_CREATED");
        assertThat(message.getEventVersion()).isEqualTo("1.0");
        assertThat(message.getEventTimestamp()).isNotNull();
    }

    @Test
    @Transactional
    void shouldPublishMultipleMessagesForMultipleOperations() throws Exception {
        // Given
        CreateAccountRequest accountRequest = CreateAccountRequest.builder()
                .customerName("Bob Johnson")
                .customerEmail("bob.johnson@example.com")
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        // When - Create account and transactions
        AccountResponse account = accountService.createAccount(accountRequest);
        
        CreateTransactionRequest deposit = CreateTransactionRequest.builder()
                .accountId(account.getId())
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("200.00"))
                .description("Deposit transaction")
                .build();

        CreateTransactionRequest withdrawal = CreateTransactionRequest.builder()
                .accountId(account.getId())
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("150.00"))
                .description("Withdrawal transaction")
                .build();

        transactionService.createTransaction(deposit);
        transactionService.createTransaction(withdrawal);

        // Then - Verify all messages were published
        // Account created message
        ConsumerRecords<String, AccountCreatedMessage> accountRecords = accountConsumer.poll(Duration.ofSeconds(5));
        assertThat(accountRecords).isNotEmpty();
        AccountCreatedMessage accountMessage = accountRecords.iterator().next().value();
        assertThat(accountMessage.getCustomerName()).isEqualTo("Bob Johnson");

        // Transaction messages (expecting 2)
        ConsumerRecords<String, TransactionCreatedMessage> transactionRecords = transactionConsumer.poll(Duration.ofSeconds(10));
        assertThat(transactionRecords).hasSize(2);
        
        // Verify deposit transaction message
        TransactionCreatedMessage firstTransaction = transactionRecords.iterator().next().value();
        assertThat(firstTransaction.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(firstTransaction.getAmount()).isEqualTo(new BigDecimal("200.00"));
    }

    private Consumer<String, AccountCreatedMessage> createAccountConsumer() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-account-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portfolio.demo.dto.message");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AccountCreatedMessage.class.getName());

        ConsumerFactory<String, AccountCreatedMessage> consumerFactory = 
                new DefaultKafkaConsumerFactory<>(consumerProps);
        return consumerFactory.createConsumer();
    }

    private Consumer<String, TransactionCreatedMessage> createTransactionConsumer() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-transaction-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portfolio.demo.dto.message");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionCreatedMessage.class.getName());

        ConsumerFactory<String, TransactionCreatedMessage> consumerFactory = 
                new DefaultKafkaConsumerFactory<>(consumerProps);
        return consumerFactory.createConsumer();
    }
}
