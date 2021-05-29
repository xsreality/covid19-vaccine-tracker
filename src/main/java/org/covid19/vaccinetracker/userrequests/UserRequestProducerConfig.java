package org.covid19.vaccinetracker.userrequests;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.producer.ProducerConfig.CLIENT_ID_CONFIG;

@Configuration
public class UserRequestProducerConfig {
    private final KafkaProperties kafkaProperties;

    public UserRequestProducerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public Map<String, Object> userRequestProducerConfigs() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());

        props.put(CLIENT_ID_CONFIG, "org.covid19.user-request-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass().getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaJsonSerializer");

        return props;
    }

    @Bean
    public ProducerFactory<String, UserRequest> userRequestProducerFactory() {
        return new DefaultKafkaProducerFactory<>(userRequestProducerConfigs());
    }

    @Bean
    public KafkaTemplate<String, UserRequest> userRequestKafkaTemplate() {
        return new KafkaTemplate<>(userRequestProducerFactory());
    }
}
