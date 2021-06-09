package org.covid19.vaccinetracker.persistence.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowStore;
import org.covid19.vaccinetracker.model.UsersByPincode;
import org.covid19.vaccinetracker.model.UsersByPincodeSerde;
import org.covid19.vaccinetracker.userrequests.MetadataStore;
import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.userrequests.model.DistrictSerde;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.covid19.vaccinetracker.userrequests.model.UserRequestSerde;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import static java.time.Duration.ofDays;
import static java.util.Objects.nonNull;
import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;

@Configuration
@EnableKafkaStreams
@Slf4j
public class KafkaStreamsConfig {
    @Value("${topic.user.requests}")
    private String userRequestsTopic;

    @Value("${topic.user.districts}")
    private String userDistrictsTopic;

    @Value("${topic.user.bypincode}")
    private String usersByPincodeTopic;

    private final KafkaProperties kafkaProperties;
    private final MetadataStore metadataStore;

    private static final String UNIQUE_DISTRICTS_STORE = "unique-districts-store";
    private static final String USERS_BY_PINCODE_AGGREGATE_STORE = "users-by-pincode-aggregate-store";

    public KafkaStreamsConfig(KafkaProperties kafkaProperties, MetadataStore metadataStore) {
        this.kafkaProperties = kafkaProperties;
        this.metadataStore = metadataStore;
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> kafkaStreamsProps = new HashMap<>(kafkaProperties.buildStreamsProperties());
        kafkaStreamsProps.put(APPLICATION_ID_CONFIG, "org.covid19.vaccine-tracker");
        kafkaStreamsProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaStreamsProps.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 3);
        kafkaStreamsProps.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        kafkaStreamsProps.put(StreamsConfig.TOPOLOGY_OPTIMIZATION, StreamsConfig.OPTIMIZE);
        return new KafkaStreamsConfiguration(kafkaStreamsProps);
    }

    @Bean
    public KTable<String, UserRequest> userRequestsTable(StreamsBuilder streamsBuilder) {
        return streamsBuilder.table(userRequestsTopic,
                Materialized.<String, UserRequest, KeyValueStore<Bytes, byte[]>>as(
                        Stores.inMemoryKeyValueStore("user-requests-store").name())
                        .withKeySerde(Serdes.String()).withValueSerde(new UserRequestSerde()).withCachingDisabled());
    }

    /*
     * Generate a unique list of districts from list of user requests (pincodes) and store
     * it in another stream.
     */
    @Bean
    public KTable<String, District> userDistrictsTable(StreamsBuilder streamsBuilder) {
        final Duration windowSize = ofDays(7L);

        // The StateStore UNIQUE_DISTRICTS_STORE must be built and added to the topology
        // before using it in stream processing.
        final StoreBuilder<WindowStore<District, Long>> dedupStoreBuilder = Stores.windowStoreBuilder(
                Stores.persistentWindowStore(UNIQUE_DISTRICTS_STORE, windowSize, windowSize, false),
                new DistrictSerde(),
                Serdes.Long());

        streamsBuilder.addStateStore(dedupStoreBuilder);

        userRequestsTable(streamsBuilder)
                .toStream()
                .peek((key, value) -> log.debug("districts streaming record {}", value))
                .filter((userId, userRequest) -> nonNull(userRequest.getPincodes()) && !userRequest.getPincodes().isEmpty())
                .flatMapValues((userId, userRequest) -> userRequest.getPincodes())
                .flatMapValues((userId, pincode) -> metadataStore.fetchDistrictsByPincode(pincode))
                .transform(() -> new DeduplicationTransformer<>(windowSize.toMillis(), (key, value) -> value, UNIQUE_DISTRICTS_STORE), UNIQUE_DISTRICTS_STORE)
                .map((userId, district) -> new KeyValue<>(district.getId(), district))
                .to(userDistrictsTopic, Produced.with(Serdes.Integer(), new DistrictSerde()));

        return streamsBuilder.table(userDistrictsTopic,
                Materialized.<String, District, KeyValueStore<Bytes, byte[]>>as(
                        Stores.inMemoryKeyValueStore("user-districts-inmemory-store").name()
                ).withKeySerde(Serdes.String()).withValueSerde(new DistrictSerde()).withCachingDisabled());
    }

    /*
     * Topology to convert user->[pincodes] topic to pincode->[users] topic.
     */
    @Bean
    public KTable<String, UsersByPincode> usersByPincodeTable(StreamsBuilder streamsBuilder) {
        final StoreBuilder<KeyValueStore<String, UsersByPincode>> aggregateStoreBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(USERS_BY_PINCODE_AGGREGATE_STORE),
                Serdes.String(), new UsersByPincodeSerde());

        streamsBuilder.addStateStore(aggregateStoreBuilder);

        userRequestsTable(streamsBuilder)
                .toStream()
                .peek((key, value) -> log.debug("streaming record {}", value))
                .filter((userId, userRequest) -> nonNull(userRequest.getPincodes()))
                .transform(() -> new UsersByPincodeTransformer(USERS_BY_PINCODE_AGGREGATE_STORE, metadataStore),
                        USERS_BY_PINCODE_AGGREGATE_STORE)
                .to(usersByPincodeTopic, Produced.with(Serdes.String(), new UsersByPincodeSerde()));

        return streamsBuilder.table(usersByPincodeTopic,
                Materialized.<String, UsersByPincode, KeyValueStore<Bytes, byte[]>>as(
                        Stores.inMemoryKeyValueStore("user-by-pincodes-inmemory").name()
                ).withKeySerde(Serdes.String()).withValueSerde(new UsersByPincodeSerde()).withCachingDisabled());
    }
}
