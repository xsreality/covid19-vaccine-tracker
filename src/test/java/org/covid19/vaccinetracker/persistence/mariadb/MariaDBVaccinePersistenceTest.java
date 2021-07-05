package org.covid19.vaccinetracker.persistence.mariadb;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.SessionEntity;
import org.covid19.vaccinetracker.persistence.mariadb.repository.CenterRepository;
import org.covid19.vaccinetracker.persistence.mariadb.repository.SessionRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class MariaDBVaccinePersistenceTest {
    @Autowired
    private CenterRepository centerRepository;
    @Autowired
    private SessionRepository sessionRepository;

    private VaccinePersistence vaccinePersistence;

    @BeforeEach
    public void beforeSetup() {
        this.vaccinePersistence = new MariaDBVaccinePersistence(centerRepository, sessionRepository
        );
    }

    @Test
    public void testFetchVaccineCentersByPincode() {
        VaccineCenters expected = new VaccineCenters();
        expected.setCenters(emptyList());
        assertEquals(expected, vaccinePersistence.fetchVaccineCentersByPincode("123456"));

        expected = new VaccineCenters();
        expected.setCenters(
                singletonList(Center.builder()
                        .centerId(383358)
                        .name("Gamhariya PHC")
                        .stateName("Bihar")
                        .districtName("Madhepura")
                        .pincode(852108)
                        .feeType("Free")
                        .sessions(singletonList(Session.builder()
                                .sessionId("001813bc-1607-42d9-9ef6-e58ba4e42d1d")
                                .date("23-05-2021")
                                .availableCapacity(98)
                                .availableCapacityDose1(48)
                                .availableCapacityDose2(50)
                                .minAgeLimit(45)
                                .vaccine("COVISHIELD")
                                .build()))
                        .build()));
        assertEquals(expected, vaccinePersistence.fetchVaccineCentersByPincode("852108"));
    }

    @Test
    public void testMarkProcessed() {
        final VaccineCenters vaccineCenters = buildVaccineCenters();
        vaccinePersistence.markProcessed(vaccineCenters);
        final Optional<SessionEntity> session = sessionRepository.findById("32bbb37e-7cb4-4942-bd92-ac56d86490f9");
        assertTrue(session.isPresent());
        assertNotNull(session.get().getProcessedAt());
    }

    @Test
    public void testPersistVaccineCenters() {
        final VaccineCenters vaccineCenters = buildVaccineCenters();
        vaccinePersistence.persistVaccineCenters(vaccineCenters);
        assertTrue(sessionRepository.findById("32bbb37e-7cb4-4942-bd92-ac56d86490f9").isPresent());
        assertTrue(centerRepository.findById(1205L).isPresent());
    }

    @Test
    public void testFindLatestSession() {
        vaccinePersistence.persistVaccineCenters(buildVaccineCenters());
        final Optional<SessionEntity> session = vaccinePersistence.findExistingSession(1205L, "22-05-2021", 18, "COVAXIN");
        assertTrue(session.isPresent());
        assertEquals("22-05-2021", session.get().getDate());
        assertEquals(18, session.get().getMinAgeLimit());
        assertEquals("COVAXIN", session.get().getVaccine());

        final Optional<SessionEntity> shouldNotExist = vaccinePersistence.findExistingSession(1205L, "23-05-2021", 18, "COVAXIN");
        assertFalse(shouldNotExist.isPresent());
    }

    @NotNull
    private VaccineCenters buildVaccineCenters() {
        final VaccineCenters vaccineCenters = new VaccineCenters();
        vaccineCenters.setCenters(
                singletonList(Center.builder()
                        .centerId(1205)
                        .name("Mohalla Clinic Peeragarhi PHC")
                        .stateName("Delhi")
                        .districtName("West Delhi")
                        .pincode(110056)
                        .feeType("Free")
                        .sessions(singletonList(Session.builder()
                                .sessionId("32bbb37e-7cb4-4942-bd92-ac56d86490f9")
                                .date("22-05-2021")
                                .availableCapacity(0)
                                .availableCapacityDose1(0)
                                .availableCapacityDose2(0)
                                .minAgeLimit(18)
                                .vaccine("COVAXIN")
                                .build()))
                        .build()));
        return vaccineCenters;
    }
}
