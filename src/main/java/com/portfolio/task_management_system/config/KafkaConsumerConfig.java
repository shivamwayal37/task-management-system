package com.portfolio.task_management_system.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.portfolio.task_management_system.event.TaskUpdatedEvent;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.security.protocol:PLAINTEXT}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.sasl.mechanism:}")
    private String saslMechanism;

    @Value("${spring.kafka.properties.sasl.jaas.config:}")
    private String saslJaasConfig;

    @Value("${spring.kafka.ssl.trust-store-location:}")
    private String trustStoreLocation;

    @Value("${spring.kafka.ssl.trust-store-password:}")
    private String trustStorePassword;

    @Value("${spring.kafka.ssl.trust-store-type:JKS}")
    private String trustStoreType;

    @Value("${spring.kafka.ssl.key-store-location:}")
    private String keyStoreLocation;

    @Value("${spring.kafka.ssl.key-store-password:}")
    private String keyStorePassword;

    @Value("${spring.kafka.ssl.key-store-type:PKCS12}")
    private String keyStoreType;

    @Value("${spring.kafka.ssl.key-password:}")
    private String keyPassword;

    @Bean
    public ConsumerFactory<String, TaskUpdatedEvent> consumerFactory() {

        JsonDeserializer<TaskUpdatedEvent> deserializer =
                new JsonDeserializer<>(TaskUpdatedEvent.class);

        deserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class);
        applySecurityProperties(props);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TaskUpdatedEvent>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, TaskUpdatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        return factory;
    }

    private void applySecurityProperties(Map<String, Object> props) {
        putIfPresent(props, "security.protocol", securityProtocol);
        putIfPresent(props, "sasl.mechanism", saslMechanism);
        putIfPresent(props, "sasl.jaas.config", saslJaasConfig);
        putIfPresent(props, "ssl.truststore.location", normalizeLocation(trustStoreLocation));
        putIfPresent(props, "ssl.truststore.password", trustStorePassword);
        putIfPresent(props, "ssl.truststore.type", trustStoreType);
        putIfPresent(props, "ssl.keystore.location", normalizeLocation(keyStoreLocation));
        putIfPresent(props, "ssl.keystore.password", keyStorePassword);
        putIfPresent(props, "ssl.keystore.type", keyStoreType);
        putIfPresent(props, "ssl.key.password", keyPassword);
    }

    private void putIfPresent(Map<String, Object> props, String key, String value) {
        if (value != null && !value.isBlank()) {
            props.put(key, value);
        }
    }

    private String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return location;
        }
        return location.startsWith("file:") ? location.substring("file:".length()) : location;
    }
}
