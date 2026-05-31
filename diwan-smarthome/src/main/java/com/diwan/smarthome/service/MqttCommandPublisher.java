package com.diwan.smarthome.service;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class MqttCommandPublisher {

    @Value("${smarthome.mqtt.broker-url:tcp://broker.hivemq.com:1883}")
    private String brokerUrl;

    @Value("${smarthome.mqtt.client-prefix:diwan-smarthome-}")
    private String clientPrefix;

    @Value("${smarthome.mqtt.username:}")
    private String username;

    @Value("${smarthome.mqtt.password:}")
    private String password;

    @Value("${smarthome.mqtt.qos:1}")
    private int qos;

    @Value("${smarthome.mqtt.retain:false}")
    private boolean retain;

    public void publishCommand(String baseTopic, String command) {
        String topic = baseTopic.endsWith("/cmd") ? baseTopic : baseTopic + "/cmd";
        String clientId = clientPrefix + UUID.randomUUID();
        MqttClient client = null;
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(false);
            options.setCleanSession(true);
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
            }
            if (password != null && !password.isBlank()) {
                options.setPassword(password.toCharArray());
            }

            client.connect(options);
            MqttMessage message = new MqttMessage(command.getBytes(StandardCharsets.UTF_8));
            message.setQos(qos);
            message.setRetained(retain);
            client.publish(topic, message);
        } catch (MqttException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to publish MQTT command");
        } finally {
            if (client != null) {
                try {
                    if (client.isConnected()) {
                        client.disconnect();
                    }
                    client.close();
                } catch (MqttException ignored) {
                    // no-op
                }
            }
        }
    }
}
