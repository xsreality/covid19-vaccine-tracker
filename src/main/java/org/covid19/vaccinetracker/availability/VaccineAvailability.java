package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.bot.BotService;
import org.covid19.vaccinetracker.cowin.CowinApiClient;
import org.covid19.vaccinetracker.cowin.CowinException;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.notifications.VaccineCentersNotification;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.District;
import org.covid19.vaccinetracker.reconciliation.PincodeReconciliation;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;

@Slf4j
@Service
public class VaccineAvailability {
    private final CowinApiClient cowinApiClient;
    private final VaccinePersistence vaccinePersistence;
    private final VaccineCentersProcessor vaccineCentersProcessor;
    private final UserRequestManager userRequestManager;
    private final AvailabilityStats availabilityStats;
    private final VaccineCentersNotification vaccineCentersNotification;
    private final PincodeReconciliation pincodeReconciliation;
    private final BotService botService;

    public VaccineAvailability(CowinApiClient cowinApiClient, VaccinePersistence vaccinePersistence,
                               VaccineCentersProcessor vaccineCentersProcessor, UserRequestManager userRequestManager,
                               AvailabilityStats availabilityStats, VaccineCentersNotification vaccineCentersNotification, PincodeReconciliation pincodeReconciliation, BotService botService) {
        this.cowinApiClient = cowinApiClient;
        this.vaccinePersistence = vaccinePersistence;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.userRequestManager = userRequestManager;
        this.availabilityStats = availabilityStats;
        this.vaccineCentersNotification = vaccineCentersNotification;
        this.pincodeReconciliation = pincodeReconciliation;
        this.botService = botService;
    }

    @Scheduled(cron = "0 0/5 6-23 * * *", zone = "IST")
    public void refreshVaccineAvailabilityFromCowinAndTriggerNotifications() {
        Executors.newSingleThreadExecutor().submit(() -> {
//            this.refreshVaccineAvailabilityFromCowin(userRequestManager.fetchAllUserRequests());
            this.refreshVaccineAvailabilityFromCowinViaKafka();
            this.vaccineCentersNotification.checkUpdatesAndSendNotifications();
            this.pincodeReconciliation.reconcilePincodesFromCowin(userRequestManager.fetchAllUserRequests());
        });
    }

    public void refreshVaccineAvailabilityFromCowinViaKafka() {
        log.info("Refreshing Vaccine Availability from Cowin API");
        availabilityStats.reset();
        availabilityStats.noteStartTime();

        this.userRequestManager.fetchAllUserDistricts()
                .parallelStream()
                .peek(district -> availabilityStats.incrementProcessedDistricts())
                .map(district -> cowinApiClient.fetchSessionsByDistrict(district.getId()))
                .peek(vaccineCenters -> {
                    availabilityStats.incrementTotalApiCalls();
                    if (isNull(vaccineCenters)) {
                        availabilityStats.incrementFailedApiCalls();
                    }
                    introduceDelay();
                })
                .filter(vaccineCentersProcessor::areVaccineCentersAvailable)
                .filter(vaccineCentersProcessor::areVaccineCentersAvailableFor18plus)
                .forEach(vaccinePersistence::persistVaccineCenters);

        availabilityStats.noteEndTime();
        final String message = String.format("[AVAILABILITY] Districts: %d, Total API calls: %d, Failed API calls: %d, Time taken: %s",
                availabilityStats.processedDistricts(), availabilityStats.totalApiCalls(),
                availabilityStats.failedApiCalls(), availabilityStats.timeTaken());
        log.info(message);
        botService.notifyOwner(message);
    }

    public void refreshVaccineAvailabilityFromCowin(List<UserRequest> userRequests) {
        log.info("Refreshing Vaccine Availability from Cowin API");
        final List<String> processedPincodes = new ArrayList<>();
        final List<District> processedDistricts = new ArrayList<>();
        availabilityStats.reset();
        availabilityStats.noteStartTime();
        userRequests.forEach(userRequest -> {
            final List<String> pincodes = userRequest.getPincodes();
            pincodes.forEach(pincode -> {
                log.debug("Processing pincode {}", pincode);
                if (processedPincodes.contains(pincode)) {
                    log.debug("Pincode {} processed already, skipping...", pincode);
                    return;
                }
                final List<District> districtsOfPincode = vaccinePersistence.fetchDistrictsByPincode(pincode);
                if (districtsOfPincode.isEmpty()) {
                    log.debug("No districts found for pincode {} in DB. Needs reconciliation.", pincode);
                    availabilityStats.incrementUnknownPincodes();
                } else {
                    availabilityStats.incrementProcessedPincodes();
                }
                districtsOfPincode.forEach(district -> {
                    if (processedDistricts.contains(district)) {
                        log.debug("District {} processed already, skipping...", district.getDistrictName());
                        return;
                    }
                    availabilityStats.incrementProcessedDistricts();
                    final VaccineCenters vaccineCenters = cowinApiClient.fetchSessionsByDistrict(district.getId());
                    availabilityStats.incrementTotalApiCalls();
                    if (isNull(vaccineCenters) || vaccineCenters.centers.isEmpty()) {
                        if (isNull(vaccineCenters)) {
                            availabilityStats.incrementFailedApiCalls();
                        }
                        log.debug("No centers found for district {} triggered by pincode {}", district, pincode);
                        return;
                    }
                    introduceDelay();
                    if (!vaccineCentersProcessor.areVaccineCentersAvailable(vaccineCenters)) {
                        log.debug("Found no centers for district {} triggered by pincode {}", district.getDistrictName(), pincode);
                        processedDistricts.add(district);
                        return;
                    }
                    if (!vaccineCentersProcessor.areVaccineCentersAvailableFor18plus(vaccineCenters)) {
                        log.debug("Vaccine centers not found for 18+ or no capacity for district {} triggered by pincode {}", district.getDistrictName(), pincode);
                        processedDistricts.add(district);
                        return;
                    }
                    log.debug("Persisting vaccine availability for district {} triggered by pincode {}", district.getDistrictName(), pincode);
                    vaccinePersistence.persistVaccineCenters(vaccineCenters);
                    processedDistricts.add(district);
                });
                processedPincodes.add(pincode);
                log.debug("Processing of pincode {} completed", pincode);
            });
        });
        availabilityStats.noteEndTime();
        log.info("[AVAILABILITY] Pincodes: {}, Districts: {}, Total API calls: {}, Failed API calls: {}, Unknown pincodes: {}, Time taken: {}",
                availabilityStats.processedPincodes(), availabilityStats.processedDistricts(),
                availabilityStats.totalApiCalls(), availabilityStats.failedApiCalls(), availabilityStats.unknownPincodes(), availabilityStats.timeTaken());
        botService.notifyOwner(String.format("[AVAILABILITY] Pincodes: %d, Districts: %d, Total API calls: %d, Failed API calls: %d, Unknown pincodes: %d, Time taken: %s",
                availabilityStats.processedPincodes(), availabilityStats.processedDistricts(),
                availabilityStats.totalApiCalls(), availabilityStats.failedApiCalls(), availabilityStats.unknownPincodes(), availabilityStats.timeTaken()));
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

    /**
     * Return pincodes that are requested by users but not available in DB.
     *
     * @return List of pincodes
     */
    public List<String> missingPincodes() {
        return userRequestManager.fetchAllUserRequests()
                .stream()
                .flatMap(userRequest -> userRequest.getPincodes().stream())
                .filter(pincode -> !vaccinePersistence.pincodeExists(pincode))
                .collect(Collectors.toList());
    }

    private void introduceDelay() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // eat
        }
    }
}
