package org.covid19.vaccinetracker.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class VaccineCentersSerde extends Serdes.WrapperSerde<VaccineCenters> {

    public VaccineCentersSerde() {
        super(new Serializer<VaccineCenters>() {
            private Gson gson = new GsonBuilder().serializeNulls().create();

            @Override
            public byte[] serialize(String s, VaccineCenters vaccineCenters) {
                return gson.toJson(vaccineCenters).getBytes(StandardCharsets.UTF_8);
            }
        }, new Deserializer<VaccineCenters>() {
            private Gson gson = new Gson();

            @Override
            public VaccineCenters deserialize(String s, byte[] bytes) {
                if (bytes == null) { // handle tombstone records.
                    return null;
                }
                return gson.fromJson(new String(bytes), VaccineCenters.class);
            }
        });
    }
}

