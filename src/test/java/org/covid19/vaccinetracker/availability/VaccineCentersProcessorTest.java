package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VaccineCentersProcessorTest {
    private VaccineCentersProcessor processor;

    @Mock
    private VaccinePersistence vaccinePersistence;

    @Mock
    private KafkaTemplate<String, String> updatedPincodesKafkaTemplate;

    @BeforeEach
    public void setup() {
        processor = new VaccineCentersProcessor(vaccinePersistence);
    }

    @Test
    public void testHasCapacity() {
        //
        assertTrue(processor.hasCapacity(Session.builder()
                .availableCapacity(5)
                .availableCapacityDose1(5)
                .availableCapacityDose2(0)
                .build()), "True if AC=dose1+dose2");
        assertTrue(processor.hasCapacity(Session.builder()
                .availableCapacity(8)
                .availableCapacityDose1(5)
                .availableCapacityDose2(3)
                .build()), "True if AC=dose1+dose2");
        assertFalse(processor.hasCapacity(Session.builder()
                .availableCapacity(8)
                .availableCapacityDose1(0)
                .availableCapacityDose2(0)
                .build()), "False if dose1 and dose2 are zero");
        assertFalse(processor.hasCapacity(Session.builder()
                .availableCapacity(8)
                .availableCapacityDose1(0)
                .availableCapacityDose2(8)
                .build()), "False if dose1 is zero (ignore dose2)");
    }
}
