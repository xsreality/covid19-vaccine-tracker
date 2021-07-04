package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.availability.aws.CowinLambdaWrapper;
import org.covid19.vaccinetracker.notifications.bot.BotService;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VaccineAvailability {
    private final VaccinePersistence vaccinePersistence;
    private final UserRequestManager userRequestManager;
    private final AvailabilityStats availabilityStats;
    private final BotService botService;
    private final CowinLambdaWrapper cowinLambdaWrapper;
    private final AvailabilityConfig config;

    public VaccineAvailability(VaccinePersistence vaccinePersistence,
                               UserRequestManager userRequestManager,
                               AvailabilityStats availabilityStats,
                               BotService botService, CowinLambdaWrapper cowinLambdaWrapper, AvailabilityConfig config) {
        this.vaccinePersistence = vaccinePersistence;
        this.userRequestManager = userRequestManager;
        this.availabilityStats = availabilityStats;
        this.botService = botService;
        this.cowinLambdaWrapper = cowinLambdaWrapper;
        this.config = config;
    }

    @Scheduled(cron = "${jobs.cron.vaccine.availability:-}", zone = "IST")
    public void refreshVaccineAvailabilityFromCowinAndTriggerNotifications() {
        Executors.newSingleThreadExecutor().submit(this::refreshVaccineAvailabilityFromCowinViaLambdaAsync);
    }

    public void refreshVaccineAvailabilityFromCowinViaLambdaAsync() {
        log.info("Refreshing Vaccine Availability from Cowin API via AWS Lambda asynchronously");
        availabilityStats.reset();
        availabilityStats.noteStartTime();

        this.userRequestManager.fetchAllUserDistricts()
                .parallelStream()
                .filter(Objects::nonNull)
                .filter(this::nonPriorityDistrict)
                .peek(district -> availabilityStats.incrementProcessedDistricts())
                .peek(district -> log.debug("processing district id {}", district.getId()))
                .forEach(district -> cowinLambdaWrapper.processDistrict(district.getId()));

        availabilityStats.noteEndTime();
        final String message = String.format("[AVAILABILITY] Districts: %d, Time taken: %s", availabilityStats.processedDistricts(), availabilityStats.timeTaken());
        log.info(message);
        botService.notifyOwner(message);
    }

    private boolean nonPriorityDistrict(District district) {
        return !config.getPriorityDistricts().contains(String.valueOf(district.getId()));
    }

    @Scheduled(cron = "${jobs.cron.db.cleanup:-}", zone = "IST")
    public void cleanupOldVaccineCenters() {
        String yesterday = Utils.yesterdayIST();
        log.info("Deleting Vaccine centers for {}", yesterday);
        this.vaccinePersistence.cleanupOldCenters(yesterday);
    }

    @Scheduled(cron = "${jobs.cron.user.stats:-}", zone = "IST")
    public void userStats() {
        int size = userRequestManager.userRequestSize();
        log.info("Users count: {}", size);
        botService.notifyOwner(String.format("User count: %d", size));
    }
}
