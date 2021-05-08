package org.covid19.vaccinetracker.persistence.kafka;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.covid19.vaccinetracker.model.UserRequest;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class KafkaStateStores {
    private ReadOnlyKeyValueStore<String, UserRequest> userRequestsStore;

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
                                    KTable<String, UserRequest> userRequestsTable) {
        return args -> {
            latch(fb).await(100, TimeUnit.SECONDS);
            userRequestsStore = fb.getKafkaStreams().store(
                    StoreQueryParameters.fromNameAndType(userRequestsTable.queryableStoreName(), QueryableStoreTypes.keyValueStore()));
        };
    }

    public KeyValueIterator<String, UserRequest> userRequests() {
        return userRequestsStore.all();
    }
}
