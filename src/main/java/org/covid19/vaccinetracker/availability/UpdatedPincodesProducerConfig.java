package org.covid19.vaccinetracker.availability;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class UpdatedPincodesProducerConfig {
    private final KafkaProperties kafkaProperties;

    public UpdatedPincodesProducerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public Map<String, Object> updatedPincodesProducerConfigs() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());

        props.put(ProducerConfig.CLIENT_ID_CONFIG, "org.covid19.updated-pincodes-producer");
        props.put(ProducerConfig.LINGER_MS_CONFIG, "5");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass().getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaJsonSerializer");

        return props;
    }

    @Bean
    public ProducerFactory<String, String> updatedPincodesProducerFactory() {
        return new DefaultKafkaProducerFactory<>(updatedPincodesProducerConfigs());
    }

    @Bean
    public KafkaTemplate<String, String> updatedPincodesKafkaTemplate() {
        return new KafkaTemplate<>(updatedPincodesProducerFactory());
    }
}
