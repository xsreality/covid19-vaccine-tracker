package org.covid19.vaccinetracker.persistence;

import org.covid19.vaccinetracker.model.VaccineCenters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaVaccinePersistence implements VaccinePersistence {
    @Value("${topic.vaccine.centers}")
    private String vaccineCentersTopic;

    private final KafkaTemplate<String, VaccineCenters> kafkaTemplate;
    private final KafkaStateStores kafkaStateStores;

    public KafkaVaccinePersistence(KafkaTemplate<String, VaccineCenters> kafkaTemplate,
                                   KafkaStateStores kafkaStateStores) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaStateStores = kafkaStateStores;
    }

    @Override
    public void persistVaccineCenters(String pincode, VaccineCenters vaccineCenters) {
        kafkaTemplate.send(vaccineCentersTopic, pincode, vaccineCenters);
    }

    @Override
    public VaccineCenters fetchVaccineCentersByPincode(String pincode) {
        return kafkaStateStores.vaccineCentersByPincode(pincode);
    }
}
