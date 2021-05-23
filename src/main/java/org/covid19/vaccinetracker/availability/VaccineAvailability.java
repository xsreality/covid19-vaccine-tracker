package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.bot.BotService;
import org.covid19.vaccinetracker.cowin.CowinApiClient;
import org.covid19.vaccinetracker.notifications.VaccineCentersNotification;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
    private final BotService botService;

    public VaccineAvailability(CowinApiClient cowinApiClient, VaccinePersistence vaccinePersistence,
                               VaccineCentersProcessor vaccineCentersProcessor, UserRequestManager userRequestManager,
                               AvailabilityStats availabilityStats, VaccineCentersNotification vaccineCentersNotification, BotService botService) {
        this.cowinApiClient = cowinApiClient;
        this.vaccinePersistence = vaccinePersistence;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.userRequestManager = userRequestManager;
        this.availabilityStats = availabilityStats;
        this.vaccineCentersNotification = vaccineCentersNotification;
        this.botService = botService;
    }

    @Scheduled(cron = "${jobs.cron.vaccine.availability:-}", zone = "IST")
    public void refreshVaccineAvailabilityFromCowinAndTriggerNotifications() {
        Executors.newSingleThreadExecutor().submit(() -> {
            this.refreshVaccineAvailabilityFromCowinViaKafka();
            this.vaccineCentersNotification.checkUpdatesAndSendNotifications();
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
        final String message = String.format("[AVAILABILITY] Districts: %d, Total API calls: %d (protected=%s), Failed API calls: %d, Time taken: %s",
                availabilityStats.processedDistricts(), availabilityStats.totalApiCalls(), cowinApiClient.isProtected(),
                availabilityStats.failedApiCalls(), availabilityStats.timeTaken());
        log.info(message);
        botService.notifyOwner(message);
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
                .distinct()
                .filter(pincode -> !vaccinePersistence.pincodeExists(pincode))
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "${jobs.cron.db.cleanup:-}", zone = "IST")
    public void cleanupOldVaccineCenters() {
        String yesterday = Utils.yesterdayIST();
        log.info("Deleting Vaccine centers for {}", yesterday);
        this.vaccinePersistence.cleanupOldCenters(yesterday);
    }

    private void introduceDelay() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // eat
        }
    }
}
