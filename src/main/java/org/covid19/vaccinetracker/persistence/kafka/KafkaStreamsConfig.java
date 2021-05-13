package org.covid19.vaccinetracker.persistence.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.UserRequestSerde;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;

@Configuration
@EnableKafkaStreams
@Slf4j
public class KafkaStreamsConfig {
    @Value("${topic.user.requests}")
    private String userRequestsTopic;

    private final KafkaProperties kafkaProperties;

    public KafkaStreamsConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
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
}
