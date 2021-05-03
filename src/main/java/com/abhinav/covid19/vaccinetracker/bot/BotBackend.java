package com.abhinav.covid19.vaccinetracker.bot;

import com.abhinav.covid19.vaccinetracker.model.UserRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BotBackend {
    @Value("{user.requests.topic")
    private String userRequestsTopic;

    private final KafkaTemplate<String, UserRequest> kafkaTemplate;

    public BotBackend(KafkaTemplate<String, UserRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void acceptUserRequest(String userId, String pincode) {
        UserRequest request = new UserRequest(userId, pincode);
        kafkaTemplate.send(userRequestsTopic, userId, request);
    }
}
