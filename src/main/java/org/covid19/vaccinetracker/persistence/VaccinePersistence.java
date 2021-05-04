package org.covid19.vaccinetracker.persistence;

import org.covid19.vaccinetracker.model.VaccineCenters;

import java.util.List;

public interface VaccinePersistence {
    void persistVaccineCenters(String pincode, VaccineCenters vaccineCenters);

    VaccineCenters fetchVaccineCentersByPincode(String pincode);

    List<VaccineCenters> fetchAllVaccineCenters();
}
