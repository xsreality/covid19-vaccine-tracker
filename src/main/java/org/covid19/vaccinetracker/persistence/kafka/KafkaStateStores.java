package org.covid19.vaccinetracker.persistence.kafka;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.covid19.vaccinetracker.model.UsersByPincode;
import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.covid19.vaccinetracker.userrequests.model.Vaccine;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_18_44;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_1;

@Slf4j
@Configuration
public class KafkaStateStores {
    private ReadOnlyKeyValueStore<String, UserRequest> userRequestsStore;
    private ReadOnlyKeyValueStore<String, District> userDistrictsStore;
    private ReadOnlyKeyValueStore<String, UsersByPincode> usersByPincodeStore;

    @Bean
    public CountDownLatch latch(StreamsBuilderFactoryBean fb) {
        CountDownLatch latch = new CountDownLatch(1);
        fb.setStateListener((newState, oldState) -> {
            if (KafkaStreams.State.RUNNING.equals(newState)) {
                latch.countDown();
            }
        });
        return latch;
    }

    @Bean
    public ApplicationRunner runner(StreamsBuilderFactoryBean fb,
                                    KTable<String, UserRequest> userRequestsTable,
                                    KTable<String, District> userDistrictsTable,
                                    KTable<String, UsersByPincode> usersByPincodeTable) {
        return args -> {
            latch(fb).await(100, TimeUnit.SECONDS);
            userRequestsStore = fb.getKafkaStreams().store(
                    StoreQueryParameters.fromNameAndType(userRequestsTable.queryableStoreName(), QueryableStoreTypes.keyValueStore()));
            userDistrictsStore = fb.getKafkaStreams().store(
                    StoreQueryParameters.fromNameAndType(userDistrictsTable.queryableStoreName(), QueryableStoreTypes.keyValueStore()));
            usersByPincodeStore = fb.getKafkaStreams().store(
                    StoreQueryParameters.fromNameAndType(usersByPincodeTable.queryableStoreName(), QueryableStoreTypes.keyValueStore()));
        };
    }

    public KeyValueIterator<String, UserRequest> userRequests() {
        return userRequestsStore.all();
    }

    public Optional<UserRequest> userRequestById(String userId) {
        if (isNull(userRequestsStore)) {
            return Optional.empty();
        }
        return Optional.ofNullable(userId)
                .map(s -> userRequestsStore.get(userId));
    }

    public List<String> pincodesForUser(String userId) {
        return ofNullable(userRequestsStore.get(userId))
                .orElseGet(() -> new UserRequest(userId, List.of(), List.of(), AGE_18_44.toString(), DOSE_1.toString(), Vaccine.ALL.toString(), null))
                .getPincodes();
    }

    public KeyValueIterator<String, District> userDistricts() {
        return userDistrictsStore.all();
    }

    public KeyValueIterator<String, UsersByPincode> usersByPincode() {
        return usersByPincodeStore.all();
    }

    public UsersByPincode usersByPincode(String pincode) {
        return usersByPincodeStore.get(pincode);
    }
}
