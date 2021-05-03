package org.covid19.vaccinetracker.tracker;

import org.covid19.vaccinetracker.cowin.CowinApiClient;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VaccineTracker {
    private final CowinApiClient cowinApiClient;
    private final VaccinePersistence vaccinePersistence;
    private final VaccineCentersProcessor vaccineCentersProcessor;

    public VaccineTracker(CowinApiClient cowinApiClient, VaccinePersistence vaccinePersistence,
                          VaccineCentersProcessor vaccineCentersProcessor) {
        this.cowinApiClient = cowinApiClient;
        this.vaccinePersistence = vaccinePersistence;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
    }

    public void refreshVaccineAvailability(String pincode) {
        log.info("Fetching vaccine availability for pin code {}", pincode);
        final VaccineCenters vaccineCenters = cowinApiClient.fetchCentersByPincode(pincode);
        if (!vaccineCentersProcessor.areVaccineCentersAvailable(vaccineCenters)) {
            log.info("Found no centers for pin code {}", pincode);
            return;
        }
        if (!vaccineCentersProcessor.areVaccineCentersAvailableFor18plus(vaccineCenters)) {
            log.info("Vaccine centers not for 18+ or no capacity for pin code {}", pincode);
            return;
        }
        log.info("Persisting vaccine availability for pin code {}", pincode);
        vaccinePersistence.persistVaccineCenters(pincode, vaccineCenters);
    }

    public VaccineCenters fetchVaccineAvailability(String pincode) {
        log.info("Fetching Vaccine availability from local store");
        return vaccinePersistence.fetchVaccineCentersByPincode(pincode);
    }
}
