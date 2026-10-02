package com.diwan.common.autoconfigure;

import com.diwan.common.kafka.DltPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaReliabilityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class, KafkaReliabilityAutoConfiguration.class))
            .withPropertyValues("spring.kafka.bootstrap-servers=localhost:1");

    @Test
    void failedRecordsGetRetriesAndADeadLetterTopic() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(DltPublisher.class);
            assertThat(ctx).hasSingleBean(CommonErrorHandler.class);
            assertThat(ctx.getBean(CommonErrorHandler.class)).isInstanceOf(DefaultErrorHandler.class);
        });
    }

    @Test
    void doesNotReplaceSpringBootsOwnKafkaTemplate() {
        runner.run(ctx -> assertThat(ctx).hasBean("kafkaTemplate"));
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("diwan.kafka.dlt.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(DltPublisher.class));
    }
}
