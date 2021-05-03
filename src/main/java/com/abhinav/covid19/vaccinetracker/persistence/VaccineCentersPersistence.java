package com.abhinav.covid19.vaccinetracker.persistence;

import com.abhinav.covid19.vaccinetracker.VaccineCenters;

public interface VaccineCentersPersistence {
    void persistVaccineCenters(String pincode, VaccineCenters vaccineCenters);

    VaccineCenters fetchVaccineCentersByPincode(String pincode);
}
