package com.diwan.users.outbox;

import com.diwan.users.entity.User;
import com.diwan.users.repository.UserRepository;
import com.diwan.users.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Real MySQL + real Kafka: deactivating a user must produce exactly the event on the topic, written through
 * the outbox table, and the outbox row must end up marked as published.
 */
@SpringBootTest(properties = {
        "diwan.outbox.poll-interval-ms=200",
        "diwan.kafka.topics.user-invalidated=it.user.invalidated"})
@ActiveProfiles("DEV")
@Testcontainers
class OutboxIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired OutboxEventRepository outboxRepository;

    @Test
    void deactivatingAUserPublishesOneEventAndMarksTheOutboxRowPublished() throws Exception {
        User user = new User();
        user.setFullName("Outbox Test");
        user.setEmail("outbox@test.local");
        user.setPassword("x");
        Long id = userRepository.save(user).getId();

        users.deactivateUser(id);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(outboxRepository.findAll()).hasSize(1)
                        .allSatisfy(e -> assertThat(e.getPublishedAt()).isNotNull()));

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "outbox-it",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of("it.user.invalidated"));
            List<ConsumerRecord<String, String>> received = new java.util.ArrayList<>();
            await().atMost(Duration.ofSeconds(20)).until(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(received::add);
                return !received.isEmpty();
            });

            assertThat(received).hasSize(1);
            assertThat(received.get(0).key()).isEqualTo(String.valueOf(id));
            JsonNode event = new ObjectMapper().readTree(received.get(0).value());
            assertThat(event.get("userId").asLong()).isEqualTo(id);
            assertThat(event.get("reason").asText()).isEqualTo("DEACTIVATED");
            assertThat(event.get("version").asInt()).isEqualTo(1);
            assertThat(event.get("eventId").asText()).isNotBlank();
        }
    }
}
