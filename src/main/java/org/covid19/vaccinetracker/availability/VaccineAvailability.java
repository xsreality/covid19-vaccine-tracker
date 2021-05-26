package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.availability.aws.CowinLambdaClient;
import org.covid19.vaccinetracker.bot.BotService;
import org.covid19.vaccinetracker.cowin.CowinApiClient;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.notifications.VaccineCentersNotification;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;

@Slf4j
@Service
public class VaccineAvailability {
    @Value("${topic.updated.pincodes}")
    private String updatedPincodesTopic;

    private final CowinApiClient cowinApiClient;
    private final VaccinePersistence vaccinePersistence;
    private final VaccineCentersProcessor vaccineCentersProcessor;
    private final UserRequestManager userRequestManager;
    private final AvailabilityStats availabilityStats;
    private final VaccineCentersNotification vaccineCentersNotification;
    private final BotService botService;
    private final KafkaTemplate<String, String> updatedPincodesKafkaTemplate;
    private final CowinLambdaClient cowinLambdaClient;

    public VaccineAvailability(CowinApiClient cowinApiClient, VaccinePersistence vaccinePersistence,
                               VaccineCentersProcessor vaccineCentersProcessor, UserRequestManager userRequestManager,
                               AvailabilityStats availabilityStats, VaccineCentersNotification vaccineCentersNotification, BotService botService, KafkaTemplate<String, String> updatedPincodesKafkaTemplate, CowinLambdaClient cowinLambdaClient) {
        this.cowinApiClient = cowinApiClient;
        this.vaccinePersistence = vaccinePersistence;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.userRequestManager = userRequestManager;
        this.availabilityStats = availabilityStats;
        this.vaccineCentersNotification = vaccineCentersNotification;
        this.botService = botService;
        this.updatedPincodesKafkaTemplate = updatedPincodesKafkaTemplate;
        this.cowinLambdaClient = cowinLambdaClient;
    }

    @Scheduled(cron = "${jobs.cron.vaccine.availability:-}", zone = "IST")
    public void refreshVaccineAvailabilityFromCowinAndTriggerNotifications() {
        Executors.newSingleThreadExecutor().submit(() -> {
            this.refreshVaccineAvailabilityFromCowinViaLambda();
            this.vaccineCentersNotification.checkUpdatesAndSendNotifications();
        });
    }

    public void refreshVaccineAvailabilityFromCowinViaLambda() {
        log.info("Refreshing Vaccine Availability from Cowin API via AWS Lambda");
        availabilityStats.reset();
        availabilityStats.noteStartTime();

        this.userRequestManager.fetchAllUserDistricts()
                .parallelStream()
                .filter(Objects::nonNull)
                .peek(district -> availabilityStats.incrementProcessedDistricts())
                .peek(district -> log.debug("processing district id {}", district.getId()))
                .map(district -> cowinLambdaClient.fetchSessionsByDistrict(district.getId()))
                .peek(this::measureApiCalls)
                .filter(vaccineCentersProcessor::areVaccineCentersAvailable)
                .filter(vaccineCentersProcessor::areVaccineCentersAvailableFor18plus)
                .forEach(vaccineCenters -> {
                    vaccinePersistence.persistVaccineCenters(vaccineCenters);
                    sendUpdatedPincodesToKafka(vaccineCenters);
                });

        availabilityStats.noteEndTime();
        final String message = String.format("[AVAILABILITY] Districts: %d, Total API calls: %d (protected=%s), Failed API calls: %d, Time taken: %s",
                availabilityStats.processedDistricts(), availabilityStats.totalApiCalls(), cowinApiClient.isProtected(),
                availabilityStats.failedApiCalls(), availabilityStats.timeTaken());
        log.info(message);
        botService.notifyOwner(message);
    }

    private void sendUpdatedPincodesToKafka(VaccineCenters vaccineCenters) {
        vaccineCenters.getCenters()
                .stream()
                .filter(Center::areVaccineCentersAvailableFor18plus)
                .map(Center::getPincode)
                .map(String::valueOf)
                .distinct()
                .forEach(pincode -> updatedPincodesKafkaTemplate.send(updatedPincodesTopic, pincode, pincode));
    }

    private void measureApiCalls(VaccineCenters vaccineCenters) {
        log.debug("Lambda call completed");
        availabilityStats.incrementTotalApiCalls();
        if (isNull(vaccineCenters)) {
            availabilityStats.incrementFailedApiCalls();
        }
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
}
