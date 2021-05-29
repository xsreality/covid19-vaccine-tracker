package org.covid19.vaccinetracker.persistence;

import org.covid19.vaccinetracker.model.VaccineCenters;

public interface VaccinePersistence {
    void persistVaccineCenters(VaccineCenters vaccineCenters);

    VaccineCenters fetchVaccineCentersByPincode(String pincode);

    void markProcessed(VaccineCenters vaccineCenters);

    void cleanupOldCenters(String date);
}
