package org.covid19.vaccinetracker.userrequests.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.covid19.vaccinetracker.userrequests.model.District;

import java.nio.charset.StandardCharsets;

public class DistrictSerde extends Serdes.WrapperSerde<District> {

    public DistrictSerde() {
        super(new Serializer<>() {
            private Gson gson = new GsonBuilder().serializeNulls().create();

            @Override
            public byte[] serialize(String s, District district) {
                return gson.toJson(district).getBytes(StandardCharsets.UTF_8);
            }
        }, new Deserializer<>() {
            private Gson gson = new Gson();

            @Override
            public District deserialize(String s, byte[] bytes) {
                return gson.fromJson(new String(bytes), District.class);
            }
        });
    }
}
