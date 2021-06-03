package org.covid19.vaccinetracker.notifications;

import org.covid19.vaccinetracker.availability.cowin.CowinApiClient;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.notifications.bot.BotService;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;

@SuppressWarnings("DeprecatedIsStillUsed")
@Slf4j
@Service
@Deprecated(forRemoval = true, since = "0.2.7")
public class VaccineCentersNotification {
    private final BotService botService;
    private final UserRequestManager userRequestManager;
    private final VaccinePersistence vaccinePersistence;
    private final VaccineCentersProcessor vaccineCentersProcessor;
    private final CowinApiClient cowinApiClient;
    private final NotificationStats notificationStats;

    public VaccineCentersNotification(BotService botService, UserRequestManager userRequestManager,
                                      VaccinePersistence vaccinePersistence, VaccineCentersProcessor vaccineCentersProcessor,
                                      CowinApiClient cowinApiClient, NotificationStats notificationStats) {
        this.botService = botService;
        this.userRequestManager = userRequestManager;
        this.vaccinePersistence = vaccinePersistence;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.cowinApiClient = cowinApiClient;
        this.notificationStats = notificationStats;
    }

    public void checkUpdatesAndSendNotifications() {
        log.info("Starting Vaccine Tracker Notification update...");
        notificationStats.reset();
        notificationStats.noteStartTime();
        ConcurrentHashMap<String, VaccineCenters> cache = new ConcurrentHashMap<>();
        final List<UserRequest> userRequests = userRequestManager.fetchAllUserRequests();
        userRequests.forEach(userRequest -> {
            notificationStats.incrementUserRequests();
//            final String lastNotifiedAt = userRequest.getLastNotifiedAt();
//            if (userWasNotifiedRecently(lastNotifiedAt)) {
//                log.info("Skipping sending notification to {} as they were notified already on {}", userRequest.getChatId(), lastNotifiedAt);
//                return;
//            }
            // process pin codes of each user
            userRequest.getPincodes().forEach(pincode -> {
                VaccineCenters vaccineCenters;
                if (cache.containsKey(pincode)) {
                    vaccineCenters = cache.get(pincode);
                } else {
                    notificationStats.incrementProcessedPincodes();
                    vaccineCenters = vaccinePersistence.fetchVaccineCentersByPincode(pincode);
                    if (isNull(vaccineCenters) || vaccineCenters.centers.isEmpty()) {
                        log.debug("No centers found for pin code {} in persistence store.", pincode);
                        VaccineCenters empty = new VaccineCenters();
                        empty.setCenters(new ArrayList<>());
                        cache.putIfAbsent(pincode, empty); // update local cache
                        return;
                    }
                }
                cache.putIfAbsent(pincode, vaccineCenters); // update local cache
                List<Center> eligibleCenters = vaccineCentersProcessor.eligibleVaccineCenters(vaccineCenters, userRequest.getChatId());
                if (eligibleCenters.isEmpty()) {
                    log.debug("No eligible vaccine centers found for pin code {}", pincode);
                    return;
                }
                if (botService.notifyAvailability(userRequest.getChatId(), eligibleCenters)) {
                    introduceDelay(10);
                    notificationStats.incrementNotificationsSent();
//                    userRequestManager.updateUserRequestLastNotifiedAt(userRequest, Utils.currentTime());
                } else {
                    notificationStats.incrementNotificationsErrors();
                }
                vaccinePersistence.markProcessed(vaccineCenters); // mark processed
            });
        });
        notificationStats.noteEndTime();
        log.info("[NOTIFICATION] Users: {}, Pincodes: {}, Sent: {}, Errors: {}, Time taken: {}",
                notificationStats.userRequests(), notificationStats.processedPincodes(),
                notificationStats.notificationsSent(), notificationStats.notificationsErrors(), notificationStats.timeTaken());
        botService.notifyOwner(String.format("[NOTIFICATION] Users: %d, Pincodes: %d, Sent: %d, Errors: %d, Time taken: %s",
                notificationStats.userRequests(), notificationStats.processedPincodes(),
                notificationStats.notificationsSent(), notificationStats.notificationsErrors(), notificationStats.timeTaken()));
        cache.clear(); // clear cache
    }

    //    @Scheduled(cron = "0 0/15 6-23 * * *", zone = "IST")
    public void checkUpdatesDirectlyWithCowinAndSendNotifications() {
        log.info("Starting Vaccine Availability via Cowin API Notification...");
        notificationStats.reset();
        notificationStats.noteStartTime();
        ConcurrentHashMap<String, VaccineCenters> cache = new ConcurrentHashMap<>();
        final List<UserRequest> userRequests = userRequestManager.fetchAllUserRequests();
        userRequests.forEach(userRequest -> {
            notificationStats.incrementUserRequests();
//            final String lastNotifiedAt = userRequest.getLastNotifiedAt();
//            if (userWasNotifiedRecently(lastNotifiedAt)) {
//                log.info("Skipping sending notification to {} as they were notified already on {}", userRequest.getChatId(), lastNotifiedAt);
//                return;
//            }
            // process pin codes of each user
            userRequest.getPincodes().forEach(pincode -> {
                VaccineCenters vaccineCenters;
                if (cache.containsKey(pincode)) {
                    log.debug("Found vaccine centers in local cache for pin code {}", pincode);
                    vaccineCenters = cache.get(pincode);
                } else {
                    notificationStats.incrementProcessedPincodes();
                    // fetch from Cowin API
                    vaccineCenters = cowinApiClient.fetchCentersByPincode(pincode);
                    introduceDelay(500); // to respect API rate limits
                    if (isNull(vaccineCenters) || vaccineCenters.centers.isEmpty()) {
                        if (isNull(vaccineCenters)) {
                            notificationStats.incrementfailedApiCalls();
                        }
                        log.debug("No centers found for pin code {}", pincode);
                        VaccineCenters empty = new VaccineCenters();
                        empty.setCenters(new ArrayList<>());
                        cache.putIfAbsent(pincode, empty); // update local cache
                        return;
                    }
                }
                cache.putIfAbsent(pincode, vaccineCenters); // update local cache
                List<Center> eligibleCenters = vaccineCentersProcessor.eligibleVaccineCenters(vaccineCenters, userRequest.getChatId());
                if (eligibleCenters.isEmpty()) {
                    log.debug("No eligible vaccine centers found for pin code {}", pincode);
                    return;
                }
                if (botService.notifyAvailability(userRequest.getChatId(), eligibleCenters)) {
                    notificationStats.incrementNotificationsSent();
                    userRequestManager.updateUserRequestLastNotifiedAt(userRequest, Utils.currentTime());
                }
            });
        });
        notificationStats.noteEndTime();
        log.info("User requests: {}, Processed pincodes: {}, Failed Cowin API Calls: {}, Notifications sent: {}, Time taken: {}",
                notificationStats.userRequests(), notificationStats.processedPincodes(), notificationStats.failedApiCalls(), notificationStats.notificationsSent(), notificationStats.timeTaken());
        botService.notifyOwner(String.format("User requests: %d, Processed pincodes: %d, Failed Cowin API Calls: %d, Notifications sent: %d, Time taken: %s",
                notificationStats.userRequests(), notificationStats.processedPincodes(), notificationStats.failedApiCalls(), notificationStats.notificationsSent(), notificationStats.timeTaken()));
        cache.clear();
    }

    private void introduceDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // eat
        }
    }
}
