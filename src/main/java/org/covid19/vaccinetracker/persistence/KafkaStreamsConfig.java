package org.covid19.vaccinetracker.persistence;

import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.model.VaccineCentersSerde;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableKafkaStreams
@Slf4j
public class KafkaStreamsConfig {
    private final KafkaProperties kafkaProperties;

    public KafkaStreamsConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> kafkaStreamsProps = new HashMap<>(kafkaProperties.buildStreamsProperties());
        kafkaStreamsProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaStreamsProps.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 8);
        kafkaStreamsProps.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        kafkaStreamsProps.put(StreamsConfig.TOPOLOGY_OPTIMIZATION, StreamsConfig.OPTIMIZE);
        return new KafkaStreamsConfiguration(kafkaStreamsProps);
    }


    @Bean
    public KTable<String, VaccineCenters> dailyStatsTable(StreamsBuilder streamsBuilder) {
        return streamsBuilder.table("vaccine-centers",
                Materialized.<String, VaccineCenters, KeyValueStore<Bytes, byte[]>>as(
                        Stores.inMemoryKeyValueStore("vaccine-centers-store").name())
                        .withKeySerde(Serdes.String()).withValueSerde(new VaccineCentersSerde()).withCachingDisabled());
    }
}
