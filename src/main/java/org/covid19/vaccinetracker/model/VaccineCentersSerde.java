package org.covid19.vaccinetracker.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VaccineCentersSerde extends Serdes.WrapperSerde<VaccineCenters> {

    public VaccineCentersSerde() {
        super(new Serializer<>() {
            final ObjectMapper mapper = new ObjectMapper();

            @Override
            public byte[] serialize(String s, VaccineCenters vaccineCenters) {
                try {
                    mapper.writeValueAsBytes(vaccineCenters);
                } catch (JsonProcessingException e) {
                    log.error("Error serializing vaccineCenters inside serde {}, {}", e.getMessage(), vaccineCenters);
                }
                return null;
            }
        }, new Deserializer<>() {
            final ObjectMapper mapper = new ObjectMapper();

            @Override
            public VaccineCenters deserialize(String s, byte[] bytes) {
                if (bytes == null) { // handle tombstone records.
                    return null;
                }
                try {
                    return mapper.readValue(bytes, VaccineCenters.class);
                } catch (IOException e) {
                    log.error("Error deserializing vaccineCenters inside serde {}", e.getMessage());
                }
                return null;
            }
        });
    }
}
