package org.covid19.vaccinetracker.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class UsersByPincodeSerde extends Serdes.WrapperSerde<UsersByPincode> {
    public UsersByPincodeSerde() {
        super(new Serializer<>() {
            private Gson gson = new GsonBuilder().serializeNulls().create();

            @Override
            public byte[] serialize(String s, UsersByPincode usersByPincode) {
                return gson.toJson(usersByPincode).getBytes(StandardCharsets.UTF_8);
            }
        }, new Deserializer<>() {
            private Gson gson = new Gson();

            @Override
            public UsersByPincode deserialize(String s, byte[] bytes) {
                return gson.fromJson(new String(bytes), UsersByPincode.class);
            }
        });
    }
}
