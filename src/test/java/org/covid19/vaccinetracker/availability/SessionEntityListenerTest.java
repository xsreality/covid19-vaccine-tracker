package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.persistence.mariadb.entity.SessionEntity;
import org.covid19.vaccinetracker.persistence.mariadb.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
public class SessionEntityListenerTest {
    @Autowired
    private SessionRepository sessionRepository;

    @BeforeEach
    void beforeSetup() {
        sessionRepository.deleteAllInBatch();
    }

    @Test
    void shouldLogOnUpdate() {
        SessionEntity entity = SessionEntity.builder()
                .id("1234")
                .availableCapacity(5)
                .availableCapacityDose1(3)
                .availableCapacityDose2(2)
                .vaccine("COVISHIELD")
                .processedAt(LocalDateTime.now())
                .date("18-05-2021")
                .build();
        sessionRepository.saveAndFlush(entity);

        entity.setDate("20-05-2021");
        sessionRepository.save(entity);
        sessionRepository.count();
        assertEquals(1, sessionRepository.count());
    }
}
