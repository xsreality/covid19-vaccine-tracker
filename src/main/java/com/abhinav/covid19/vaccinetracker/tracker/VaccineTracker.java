package com.abhinav.covid19.vaccinetracker.tracker;

import com.abhinav.covid19.vaccinetracker.cowin.CowinApiClient;
import com.abhinav.covid19.vaccinetracker.model.VaccineCenters;
import com.abhinav.covid19.vaccinetracker.persistence.VaccinePersistence;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VaccineTracker {
    private final CowinApiClient cowinApiClient;
    private final VaccinePersistence vaccinePersistence;

    public VaccineTracker(CowinApiClient cowinApiClient, VaccinePersistence vaccinePersistence) {
        this.cowinApiClient = cowinApiClient;
        this.vaccinePersistence = vaccinePersistence;
    }

    public void refreshVaccineAvailability(String pincode) {
        log.info("Fetching vaccine availability for pincode {}", pincode);
        final VaccineCenters vaccineCenters = cowinApiClient.fetchCentersByPincode(pincode);
        log.info("Persisting vaccine availability for pincode {}", pincode);
        vaccinePersistence.persistVaccineCenters(pincode, vaccineCenters);
    }

    public VaccineCenters fetchVaccineAvailability(String pincode) {
        log.info("Fetching Vaccine availability from local store");
        return vaccinePersistence.fetchVaccineCentersByPincode(pincode);
    }
}
