package com.diwan.common.kafka;

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Producer used only to publish failed records to their dead-letter topic. It is deliberately NOT a
 * KafkaTemplate/ProducerFactory bean, so it does not switch off Spring Boot's own Kafka auto-configuration.
 * Raw bytes (records that could not be deserialized) are re-published unchanged; objects are sent as JSON.
 */
public class DltPublisher implements DisposableBean {

    private final DefaultKafkaProducerFactory<Object, Object> factory;
    private final KafkaTemplate<Object, Object> template;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DltPublisher(KafkaProperties properties) {
        Map<String, Object> config = new LinkedHashMap<>(properties.buildProducerProperties(null));
        config.remove("key.serializer");
        config.remove("value.serializer");

        Map<Class<?>, Serializer<?>> keys = new LinkedHashMap<>();
        keys.put(byte[].class, new ByteArraySerializer());
        keys.put(Object.class, new StringSerializer());
        Map<Class<?>, Serializer<?>> values = new LinkedHashMap<>();
        values.put(byte[].class, new ByteArraySerializer());
        values.put(Object.class, new JsonSerializer<>());

        this.factory = new DefaultKafkaProducerFactory<>(config,
                (Serializer) new DelegatingByTypeSerializer(keys, true),
                (Serializer) new DelegatingByTypeSerializer(values, true));
        this.template = new KafkaTemplate<>(factory);
    }

    public KafkaTemplate<Object, Object> template() {
        return template;
    }

    @Override
    public void destroy() {
        factory.destroy();
    }
}
