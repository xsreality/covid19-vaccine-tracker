package com.abhinav.covid19.vaccinetracker.bot;

import com.abhinav.covid19.vaccinetracker.model.UserRequest;
import com.abhinav.covid19.vaccinetracker.model.VaccineCenters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BotBackend {
    @Value("{user.requests.topic")
    private String userRequestsTopic;

    private final BotService botService;
    private final KafkaTemplate<String, UserRequest> kafkaTemplate;

    public BotBackend(BotService botservice, KafkaTemplate<String, UserRequest> kafkaTemplate) {
        this.botService = botservice;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void acceptUserRequest(String userId, String pincode) {
        UserRequest request = new UserRequest(userId, pincode);
        kafkaTemplate.send(userRequestsTopic, userId, request);
    }

    public void notifyUser(String userId, VaccineCenters vaccineCenters) {
        log.info("Notifying user {} about their vaccine centers", userId);
        String message = botService.format(userId, vaccineCenters);
        botService.send(message);
    }
}
