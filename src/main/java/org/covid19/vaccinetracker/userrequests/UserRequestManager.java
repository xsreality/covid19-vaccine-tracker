package org.covid19.vaccinetracker.userrequests;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.UsersByPincode;
import org.covid19.vaccinetracker.persistence.kafka.KafkaStateStores;
import org.covid19.vaccinetracker.userrequests.model.District;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserRequestManager {
    @Value("${topic.user.requests}")
    private String userRequestsTopic;

    private final KafkaTemplate<String, UserRequest> kafkaTemplate;
    private final KafkaStateStores kafkaStateStores;
    private final MetadataStore metadataStore;

    public UserRequestManager(KafkaTemplate<String, UserRequest> kafkaTemplate, KafkaStateStores kafkaStateStores, MetadataStore metadataStore) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaStateStores = kafkaStateStores;
        this.metadataStore = metadataStore;
    }

    public int userRequestSize() {
        int size = 0;
        final KeyValueIterator<String, UserRequest> it = this.kafkaStateStores.userRequests();
        while (it.hasNext()) {
            size++;
            it.next();
        }
        return size;
    }

    public Set<District> fetchAllUserDistricts() {
        final Set<District> userDistricts = new HashSet<>();
        final KeyValueIterator<String, District> districts = this.kafkaStateStores.userDistricts();
        while (districts.hasNext()) {
            final KeyValue<String, District> request = districts.next();
            userDistricts.add(request.value);
        }
        return userDistricts;
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

    public List<UsersByPincode> fetchAllUsersByPincode() {
        final List<UsersByPincode> usersByPincodes = new ArrayList<>();
        final KeyValueIterator<String, UsersByPincode> iterator = this.kafkaStateStores.usersByPincode();
        while (iterator.hasNext()) {
            final KeyValue<String, UsersByPincode> request = iterator.next();
            usersByPincodes.add(request.value);
        }
        return usersByPincodes;
    }

    public List<String> fetchUserPincodes(String userId) {
        return kafkaStateStores.pincodesForUser(userId);
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

    public UsersByPincode fetchUsersByPincode(String pincode) {
        return this.kafkaStateStores.usersByPincode(pincode);
    }

    /**
     * Reads the user requests state store and produces each request again to the user-requests
     * topic Useful when adding new topology that needs to be populated
     */
    public void regenerateUserRequests() {
        final KeyValueIterator<String, UserRequest> requests = kafkaStateStores.userRequests();
        AtomicInteger count = new AtomicInteger();
        requests.forEachRemaining(entry -> {
            List<String> updatedPincodes = entry.value.getPincodes();
            updatedPincodes.add("999999");

            kafkaTemplate.send(userRequestsTopic, entry.key, new UserRequest(entry.value.getChatId(), updatedPincodes, entry.value.getLastNotifiedAt()));
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            updatedPincodes.removeAll(List.of("999999"));
            kafkaTemplate.send(userRequestsTopic, entry.key, entry.value);
            count.getAndIncrement();
        });
        log.info("Reloaded {} user requests", count);
    }

    /**
     * Return pincodes that are requested by users but not available in DB.
     *
     * @return List of pincodes
     */
    public List<String> missingPincodes() {
        return fetchAllUserRequests()
                .stream()
                .flatMap(userRequest -> userRequest.getPincodes().stream())
                .distinct()
                .filter(pincode -> !metadataStore.pincodeExists(pincode))
                .collect(Collectors.toList());
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
