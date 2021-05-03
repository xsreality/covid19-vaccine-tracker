package com.abhinav.covid19.vaccinetracker.persistence;

import com.abhinav.covid19.vaccinetracker.model.VaccineCenters;

public interface VaccinePersistence {
    void persistVaccineCenters(String pincode, VaccineCenters vaccineCenters);

    VaccineCenters fetchVaccineCentersByPincode(String pincode);
}
