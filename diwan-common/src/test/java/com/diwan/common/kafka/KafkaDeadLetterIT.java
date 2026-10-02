package com.diwan.common.kafka;

import com.diwan.common.autoconfigure.KafkaReliabilityAutoConfiguration;
import com.diwan.common.event.UserInvalidatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Real Kafka: poison messages and repeatedly failing records must reach the dead-letter topic, not block or vanish. */
@SpringBootTest(classes = KafkaDeadLetterIT.TestApp.class, properties = {
        "spring.kafka.consumer.group-id=dlt-it",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
        "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=com.diwan.common.event",
        "spring.kafka.consumer.properties.spring.json.value.default.type=com.diwan.common.event.UserInvalidatedEvent",
        "diwan.kafka.retry.max-retries=2",
        "diwan.kafka.retry.initial-interval-ms=50"})
@Testcontainers
class KafkaDeadLetterIT {

    static final String TOPIC = "it.events";

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    static final List<Long> processed = new CopyOnWriteArrayList<>();
    static final AtomicInteger attemptsForUser13 = new AtomicInteger();

    @Configuration
    @EnableKafka
    @ImportAutoConfiguration({KafkaAutoConfiguration.class, KafkaReliabilityAutoConfiguration.class})
    static class TestApp {
        @Bean
        Listener listener() {
            return new Listener();
        }
    }

    static class Listener {
        @KafkaListener(topics = TOPIC)
        void on(UserInvalidatedEvent event) {
            if (event.getUserId() == 13L) {
                attemptsForUser13.incrementAndGet();
                throw new IllegalStateException("cannot process user 13");
            }
            processed.add(event.getUserId());
        }
    }

    private void send(String value) throws Exception {
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            producer.send(new ProducerRecord<>(TOPIC, "k", value)).get();
        }
    }

    @Test
    void poisonAndFailingRecordsGoToTheDltAndHealthyRecordsStillFlow() throws Exception {
        send("this is not json");                                                  // cannot be deserialized
        send("{\"version\":1,\"userId\":13,\"reason\":\"DELETED\"}");              // listener always throws
        send("{\"version\":2,\"userId\":42,\"reason\":\"DELETED\",\"extra\":\"x\"}"); // healthy, unknown field

        await().atMost(Duration.ofSeconds(60)).until(() -> processed.contains(42L));
        // initial attempt + 2 retries, then dead-lettered
        await().atMost(Duration.ofSeconds(30)).until(() -> attemptsForUser13.get() == 3);

        List<ConsumerRecord<String, String>> dead = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-reader",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of(TOPIC + ".DLT"));
            await().atMost(Duration.ofSeconds(30)).until(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(dead::add);
                return dead.size() >= 2;
            });
        }

        assertThat(dead).extracting(ConsumerRecord::value)
                .anyMatch(v -> v.equals("this is not json"))          // raw bytes preserved
                .anyMatch(v -> v.contains("\"userId\":13"));          // failed record, re-published as JSON
        assertThat(attemptsForUser13.get()).isEqualTo(3);
    }
}
