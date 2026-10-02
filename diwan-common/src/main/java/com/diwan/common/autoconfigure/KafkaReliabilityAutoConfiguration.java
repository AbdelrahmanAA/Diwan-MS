package com.diwan.common.autoconfigure;

import com.diwan.common.kafka.DltPublisher;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Failed Kafka listener records are retried with exponential backoff and then published to
 * {@code <topic>.DLT} instead of being dropped. Records that cannot be deserialized go straight to the DLT
 * (use ErrorHandlingDeserializer on the consumer). Tuning:
 * <pre>
 * diwan.kafka.retry.max-retries=3
 * diwan.kafka.retry.initial-interval-ms=1000
 * diwan.kafka.dlt.enabled=false          (opt out)
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass({DefaultErrorHandler.class, KafkaProperties.class})
@ConditionalOnProperty(name = "diwan.kafka.dlt.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaReliabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DltPublisher diwanDltPublisher(KafkaProperties properties) {
        return new DltPublisher(properties);
    }

    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    public DefaultErrorHandler diwanKafkaErrorHandler(
            DltPublisher dlt,
            @Value("${diwan.kafka.retry.max-retries:3}") int maxRetries,
            @Value("${diwan.kafka.retry.initial-interval-ms:1000}") long initialIntervalMs) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                dlt.template(), (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));
        ExponentialBackOff backOff = new ExponentialBackOff(initialIntervalMs, 2.0);
        backOff.setMaxAttempts(maxRetries);
        backOff.setMaxInterval(30_000);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // Each retry is logged by Spring Kafka as a "seek to current" exception; that is routine, not an error
        handler.setLogLevel(KafkaException.Level.WARN);
        return handler;
    }
}
