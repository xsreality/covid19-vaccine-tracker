package org.covid19.vaccinetracker.bot;

import org.covid19.vaccinetracker.model.UserRequest;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BotBackend {
    @Value("${topic.user.requests}")
    private String userRequestsTopic;

    private final KafkaTemplate<String, UserRequest> kafkaTemplate;

    public BotBackend(KafkaTemplate<String, UserRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void acceptUserRequest(String userId, String pincode) {
        UserRequest request = new UserRequest(userId, pincode);
        kafkaTemplate.setProducerListener(producerListener());
        try {
            kafkaTemplate.send(userRequestsTopic, userId, request).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error producing user request to Kafka", e);
        }
    }

    @NotNull
    private ProducerListener<String, UserRequest> producerListener() {
        return new ProducerListener<>() {
            @Override
            public void onSuccess(ProducerRecord<String, UserRequest> producerRecord, RecordMetadata recordMetadata) {
                log.info("Successfully produced user request for chatId {}, request {}", producerRecord.key(), producerRecord.value());
            }

            @Override
            public void onError(ProducerRecord<String, UserRequest> producerRecord, RecordMetadata metadata, Exception exception) {
                log.error("Error producing record {}", producerRecord, exception);
            }
        };
    }
}
