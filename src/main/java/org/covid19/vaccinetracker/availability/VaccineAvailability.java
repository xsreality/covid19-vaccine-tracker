package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.bot.BotService;
import org.covid19.vaccinetracker.cowin.CowinApiClient;
import org.covid19.vaccinetracker.cowin.CowinException;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.District;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;

@Slf4j
@Service
@Transactional
public class VaccineAvailability {
    private final CowinApiClient cowinApiClient;
    private final VaccinePersistence vaccinePersistence;
    private final VaccineCentersProcessor vaccineCentersProcessor;
    private final UserRequestManager userRequestManager;
    private final AvailabilityStats availabilityStats;
    private final BotService botService;

    public VaccineAvailability(CowinApiClient cowinApiClient, VaccinePersistence vaccinePersistence,
                               VaccineCentersProcessor vaccineCentersProcessor, UserRequestManager userRequestManager,
                               AvailabilityStats availabilityStats, BotService botService) {
        this.cowinApiClient = cowinApiClient;
        this.vaccinePersistence = vaccinePersistence;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.userRequestManager = userRequestManager;
        this.availabilityStats = availabilityStats;
        this.botService = botService;
    }

    @Scheduled(cron = "0 10 * * * *")
    public void refreshVaccineAvailabilityFromCowin() {
        log.info("Refreshing Vaccine Availability from Cowin API");
        final List<String> processedPincodes = new ArrayList<>();
        final List<District> processedDistricts = new ArrayList<>();
        availabilityStats.reset();
        final List<UserRequest> userRequests = this.userRequestManager.fetchAllUserRequests();
        userRequests.forEach(userRequest -> {
            final List<String> pincodes = userRequest.getPincodes();
            pincodes.forEach(pincode -> {
                log.info("Processing pincode {}", pincode);
                if (processedPincodes.contains(pincode)) {
                    log.info("Pincode {} processed already, skipping...", pincode);
                    return;
                }
                availabilityStats.incrementProcessedPincodes();
                final List<District> districtsOfPincode = vaccinePersistence.fetchDistrictsByPincode(pincode);
                if (districtsOfPincode.isEmpty()) {
                    log.warn("No districts found for pincode {} in DB. Data needs re-work.", pincode);
                }
                districtsOfPincode.forEach(district -> {
                    if (processedDistricts.contains(district)) {
                        log.info("District {} processed already, skipping...", district.getDistrictName());
                        return;
                    }
                    availabilityStats.incrementProcessedDistricts();
                    final VaccineCenters vaccineCenters = cowinApiClient.fetchSessionsByDistrict(district.getId());
                    if (isNull(vaccineCenters) || vaccineCenters.centers.isEmpty()) {
                        if (isNull(vaccineCenters)) {
                            availabilityStats.incrementFailedApiCalls();
                        }
                        log.debug("No centers found for district {} triggered by pincode {}", district, pincode);
                        return;
                    }
                    introduceDelay();
                    if (!vaccineCentersProcessor.areVaccineCentersAvailable(vaccineCenters)) {
                        log.info("Found no centers for district {} triggered by pincode {}", district.getDistrictName(), pincode);
                        return;
                    }
                    if (!vaccineCentersProcessor.areVaccineCentersAvailableFor18plus(vaccineCenters)) {
                        processedDistricts.add(district);
                        log.info("Vaccine centers not found for 18+ or no capacity for district {} triggered by pincode {}", district.getDistrictName(), pincode);
                        return;
                    }
                    log.info("Persisting vaccine availability for district {} triggered by pincode {}", district.getDistrictName(), pincode);
                    vaccinePersistence.persistVaccineCenters(vaccineCenters);
                    processedDistricts.add(district);
                });
                processedPincodes.add(pincode);
                log.info("Processing of pincode {} completed", pincode);
            });
        });
        log.info("Refreshed pincodes: {}, Refreshed districts: {}", availabilityStats.processedPincodes(), availabilityStats.processedDistricts());
        botService.notifyOwner(String.format("Refreshed pincodes: %d, Refreshed districts: %d",
                availabilityStats.processedPincodes(), availabilityStats.processedDistricts()));
        // clear cache
        processedDistricts.clear();
        processedPincodes.clear();
    }

    public void refreshVaccineAvailabilityFromCowinApi(int districtId) {
        final VaccineCenters vaccineCenters = cowinApiClient.fetchSessionsByDistrict(districtId);
        vaccinePersistence.persistVaccineCenters(vaccineCenters);
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
        log.info("Fetching Vaccine availability from local store for pin code {}", pincode);
        return vaccinePersistence.fetchVaccineCentersByPincode(pincode);
    }

    private void introduceDelay() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // eat
        }
    }
}
