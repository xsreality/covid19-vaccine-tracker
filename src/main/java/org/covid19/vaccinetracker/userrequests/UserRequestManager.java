package org.covid19.vaccinetracker.userrequests;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.persistence.kafka.KafkaStateStores;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserRequestManager {
    @Value("${topic.user.requests}")
    private String userRequestsTopic;

    private final KafkaTemplate<String, UserRequest> kafkaTemplate;
    private final KafkaStateStores kafkaStateStores;

    public UserRequestManager(KafkaTemplate<String, UserRequest> kafkaTemplate, KafkaStateStores kafkaStateStores) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaStateStores = kafkaStateStores;
    }

    public List<UserRequest> fetchAllUserRequests() {
        final List<UserRequest> userRequests = new ArrayList<>();
        final KeyValueIterator<String, UserRequest> requests = this.kafkaStateStores.userRequests();
        while (requests.hasNext()) {
            final KeyValue<String, UserRequest> request = requests.next();
            userRequests.add(request.value);
        }
        return userRequests;
    }

    public void acceptUserRequest(String userId, List<String> pincodes) {
        UserRequest request = new UserRequest(userId, pincodes, null);
        kafkaTemplate.setProducerListener(producerListener());
        try {
            kafkaTemplate.send(userRequestsTopic, userId, request).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error producing user request to Kafka", e);
        }
    }

    public void updateUserRequestLastNotifiedAt(UserRequest userRequest, String lastNotifiedAt) {
        UserRequest updatedUserRequest = new UserRequest(userRequest.getChatId(), userRequest.getPincodes(), lastNotifiedAt);
        kafkaTemplate.setProducerListener(producerListener());
        try {
            kafkaTemplate.send(userRequestsTopic, userRequest.getChatId(), updatedUserRequest).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error producing user request to Kafka", e);
        }
    }

    @NotNull
    private ProducerListener<String, UserRequest> producerListener() {
        return new ProducerListener<>() {
            @Override
            public void onSuccess(ProducerRecord<String, UserRequest> producerRecord, RecordMetadata recordMetadata) {
                log.info("Successfully updated user request {}", producerRecord.value());
            }

            @Override
            public void onError(ProducerRecord<String, UserRequest> producerRecord, RecordMetadata metadata, Exception exception) {
                log.error("Error producing record {}", producerRecord, exception);
            }
        };
    }
}
