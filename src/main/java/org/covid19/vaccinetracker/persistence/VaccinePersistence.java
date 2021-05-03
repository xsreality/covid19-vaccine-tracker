package org.covid19.vaccinetracker.persistence;

import org.covid19.vaccinetracker.model.VaccineCenters;

public interface VaccinePersistence {
    void persistVaccineCenters(String pincode, VaccineCenters vaccineCenters);

    VaccineCenters fetchVaccineCentersByPincode(String pincode);
}
