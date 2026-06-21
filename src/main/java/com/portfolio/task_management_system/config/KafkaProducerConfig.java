package com.portfolio.task_management_system.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portfolio.task_management_system.event.TaskUpdatedEvent;

@EnableKafka
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

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
    public ProducerFactory<String, TaskUpdatedEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        applySecurityProperties(configProps);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonSerializer<TaskUpdatedEvent> jsonSerializer = new JsonSerializer<>(objectMapper);

        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, TaskUpdatedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    private void applySecurityProperties(Map<String, Object> configProps) {
        putIfPresent(configProps, "security.protocol", securityProtocol);
        putIfPresent(configProps, "sasl.mechanism", saslMechanism);
        putIfPresent(configProps, "sasl.jaas.config", saslJaasConfig);
        putIfPresent(configProps, "ssl.truststore.location", normalizeLocation(trustStoreLocation));
        putIfPresent(configProps, "ssl.truststore.password", trustStorePassword);
        putIfPresent(configProps, "ssl.truststore.type", trustStoreType);
        putIfPresent(configProps, "ssl.keystore.location", normalizeLocation(keyStoreLocation));
        putIfPresent(configProps, "ssl.keystore.password", keyStorePassword);
        putIfPresent(configProps, "ssl.keystore.type", keyStoreType);
        putIfPresent(configProps, "ssl.key.password", keyPassword);
    }

    private void putIfPresent(Map<String, Object> configProps, String key, String value) {
        if (value != null && !value.isBlank()) {
            configProps.put(key, value);
        }
    }

    private String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return location;
        }
        return location.startsWith("file:") ? location.substring("file:".length()) : location;
    }
}
