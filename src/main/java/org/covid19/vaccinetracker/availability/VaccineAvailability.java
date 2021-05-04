package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.cowin.CowinApiClient;
import org.covid19.vaccinetracker.cowin.CowinException;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VaccineAvailability {
    private final CowinApiClient cowinApiClient;
    private final VaccinePersistence vaccinePersistence;
    private final VaccineCentersProcessor vaccineCentersProcessor;
    private final UserRequestManager userRequestManager;

    public VaccineAvailability(CowinApiClient cowinApiClient, VaccinePersistence vaccinePersistence,
                               VaccineCentersProcessor vaccineCentersProcessor,
                               UserRequestManager userRequestManager) {
        this.cowinApiClient = cowinApiClient;
        this.vaccinePersistence = vaccinePersistence;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.userRequestManager = userRequestManager;
    }

    @Scheduled(cron = "0 0/30 * * * *")
    public void refreshVaccineAvailabilityFromCowin() {
        log.info("Refreshing Vaccine Availability from Cowin");
        final List<String> processedPincodes = new ArrayList<>();
        final List<UserRequest> userRequests = this.userRequestManager.fetchAllUserRequests();
        userRequests.forEach(userRequest -> {
            final List<String> pincodes = userRequest.getPincodes();
            pincodes.forEach(pincode -> {
                try {
                    if (processedPincodes.contains(pincode)) {
                        log.info("Pincode {} processed already, skipping...", pincode);
                        return;
                    }
                    final VaccineCenters vaccineCenters = cowinApiClient.fetchCentersByPincode(pincode);
                    if (!vaccineCentersProcessor.areVaccineCentersAvailable(vaccineCenters)) {
                        log.info("Found no centers for pin code {}", pincode);
                        return;
                    }
                    if (!vaccineCentersProcessor.areVaccineCentersAvailableFor18plus(vaccineCenters)) {
                        log.info("Vaccine centers not found for 18+ or no capacity for pin code {}", pincode);
                        return;
                    }
                    log.info("Persisting vaccine availability for pin code {}", pincode);
                    vaccinePersistence.persistVaccineCenters(pincode, vaccineCenters);
                    processedPincodes.add(pincode);
                } catch (CowinException ce) {
                    log.error("Error fetching centers for pincode {}, got status code {}", pincode, ce.getStatusCode());
                }
            });
        });
        processedPincodes.clear(); // clear cache
    }

    public void fetchVaccineAvailabilityFromCowinApi(String pincode) {
        try {
            log.info("Fetching vaccine availability for pin code {}", pincode);
            final VaccineCenters vaccineCenters = cowinApiClient.fetchCentersByPincode(pincode);
            if (!vaccineCentersProcessor.areVaccineCentersAvailable(vaccineCenters)) {
                log.info("Found no centers for pin code {}", pincode);
                return;
            }
            if (!vaccineCentersProcessor.areVaccineCentersAvailableFor18plus(vaccineCenters)) {
                log.info("Vaccine centers not found for 18+ or no capacity for pin code {}", pincode);
                return;
            }
            log.info("Persisting vaccine availability for pin code {}", pincode);
            vaccinePersistence.persistVaccineCenters(pincode, vaccineCenters);
        } catch (CowinException ce) {
            log.error("Error fetching centers for pincode {}, got status code {}", pincode, ce.getStatusCode());
        }
    }

    public VaccineCenters fetchVaccineAvailabilityFromPersistenceStore(String pincode) {
        log.info("Fetching Vaccine availability from local store");
        // botservice.notify(...)
        return vaccinePersistence.fetchVaccineCentersByPincode(pincode);
    }
}
