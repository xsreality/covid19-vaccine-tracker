package com.abhinav.covid19.vaccinetracker.kafka;

import com.abhinav.covid19.vaccinetracker.VaccineCenters;
import com.abhinav.covid19.vaccinetracker.persistence.VaccineCentersPersistence;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaVaccineCentersPersistence implements VaccineCentersPersistence {
    private final KafkaTemplate<String, VaccineCenters> kafkaTemplate;

    public KafkaVaccineCentersPersistence(KafkaTemplate<String, VaccineCenters> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void persistVaccineCenters(String pincode, VaccineCenters vaccineCenters) {
        kafkaTemplate.send("vaccine_centers", pincode, vaccineCenters);
    }

    @Override
    public VaccineCenters fetchVaccineCentersByPincode(String pincode) {
        log.info("TBA");
        return null;
    }
}
