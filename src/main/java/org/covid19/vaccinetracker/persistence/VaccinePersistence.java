package org.covid19.vaccinetracker.persistence;

import org.covid19.vaccinetracker.model.CenterSession;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.mariadb.entity.SessionEntity;

import java.util.List;
import java.util.Optional;

public interface VaccinePersistence {
    void persistVaccineCenters(VaccineCenters vaccineCenters);

    VaccineCenters fetchVaccineCentersByPincode(String pincode);

    void markProcessed(VaccineCenters vaccineCenters);

    void cleanupOldCenters(String date);

    Optional<SessionEntity> findExistingSession(Long centerId, String date, Integer age, String vaccine);

    List<CenterSession> findAllSessionsByPincode(String pincode);
}
