package com.abhinav.covid19.vaccinetracker.kafka;

import com.abhinav.covid19.vaccinetracker.model.VaccineCenters;
import com.abhinav.covid19.vaccinetracker.persistence.VaccinePersistence;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaVaccinePersistence implements VaccinePersistence {
    private final KafkaTemplate<String, VaccineCenters> kafkaTemplate;
    private final KafkaStateStores kafkaStateStores;

    public KafkaVaccinePersistence(KafkaTemplate<String, VaccineCenters> kafkaTemplate,
                                   KafkaStateStores kafkaStateStores) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaStateStores = kafkaStateStores;
    }

    @Override
    public void persistVaccineCenters(String pincode, VaccineCenters vaccineCenters) {
        kafkaTemplate.send("vaccine-centers", pincode, vaccineCenters);
    }

    @Override
    public VaccineCenters fetchVaccineCentersByPincode(String pincode) {
        return kafkaStateStores.vaccineCentersByPincode(pincode);
    }
}
