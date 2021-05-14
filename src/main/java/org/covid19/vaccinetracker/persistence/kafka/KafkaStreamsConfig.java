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
import org.covid19.vaccinetracker.model.DistrictSerde;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.UserRequestSerde;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.District;
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
import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;

@Configuration
@EnableKafkaStreams
@Slf4j
public class KafkaStreamsConfig {
    @Value("${topic.user.requests}")
    private String userRequestsTopic;

    @Value("${topic.user.districts}")
    private String userDistrictsTopic;

    private final KafkaProperties kafkaProperties;
    private final VaccinePersistence vaccinePersistence;

    private static final String UNIQUE_DISTRICTS_STORE = "unique-districts-store";

    public KafkaStreamsConfig(KafkaProperties kafkaProperties, VaccinePersistence vaccinePersistence) {
        this.kafkaProperties = kafkaProperties;
        this.vaccinePersistence = vaccinePersistence;
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> kafkaStreamsProps = new HashMap<>(kafkaProperties.buildStreamsProperties());
        kafkaStreamsProps.put(APPLICATION_ID_CONFIG, "org.covid19.vaccine-tracker");
        kafkaStreamsProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaStreamsProps.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
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

        userRequestsTable(streamsBuilder).toStream()
                .flatMapValues((userId, userRequest) -> userRequest.getPincodes())
                .flatMapValues((userId, pincode) -> vaccinePersistence.fetchDistrictsByPincode(pincode))
                .transform(() -> new DeduplicationTransformer<>(Duration.ofDays(365L).toMillis(), (key, value) -> value, UNIQUE_DISTRICTS_STORE), UNIQUE_DISTRICTS_STORE)
                .map((userId, district) -> new KeyValue<>(district.getId(), district))
                .to(userDistrictsTopic, Produced.with(Serdes.Integer(), new DistrictSerde()));

        return streamsBuilder.table(userDistrictsTopic,
                Materialized.<String, District, KeyValueStore<Bytes, byte[]>>as(
                        Stores.inMemoryKeyValueStore("user-districts-inmemory-store").name()
                ).withKeySerde(Serdes.String()).withValueSerde(new DistrictSerde()).withCachingDisabled());
    }
}
