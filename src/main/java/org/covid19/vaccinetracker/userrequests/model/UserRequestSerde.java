package org.covid19.vaccinetracker.userrequests.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class UserRequestSerde extends Serdes.WrapperSerde<UserRequest> {

    public UserRequestSerde() {
        super(new Serializer<>() {
            private Gson gson = new GsonBuilder().serializeNulls().create();

            @Override
            public byte[] serialize(String s, UserRequest UserRequest) {
                return gson.toJson(UserRequest).getBytes(StandardCharsets.UTF_8);
            }
        }, new Deserializer<>() {
            private Gson gson = new Gson();

            @Override
            public UserRequest deserialize(String s, byte[] bytes) {
                return gson.fromJson(new String(bytes), UserRequest.class);
            }
        });
    }
}

