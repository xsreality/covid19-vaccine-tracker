package org.covid19.vaccinetracker.notifications;

import org.covid19.vaccinetracker.availability.VaccineCentersProcessor;
import org.covid19.vaccinetracker.bot.BotService;
import org.covid19.vaccinetracker.cowin.CowinApiClient;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Service
public class VaccineCentersNotification {
    @Value("${users.over45}")
    private List<String> usersOver45;

    private final BotService botService;
    private final UserRequestManager userRequestManager;
    private final VaccinePersistence vaccinePersistence;
    private final VaccineCentersProcessor vaccineCentersProcessor;
    private final CowinApiClient cowinApiClient;

    public VaccineCentersNotification(BotService botService, UserRequestManager userRequestManager,
                                      VaccinePersistence vaccinePersistence, VaccineCentersProcessor vaccineCentersProcessor, CowinApiClient cowinApiClient) {
        this.botService = botService;
        this.userRequestManager = userRequestManager;
        this.vaccinePersistence = vaccinePersistence;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.cowinApiClient = cowinApiClient;
    }

    /*
     * 1. For each user, if vaccine centers exist and last notified
     * is beyond 24 hours, send bot message.
     * 2. Cache the bot message in a local cache temporarily to
     * send same alert faster for other users with same pin code.
     * 3. Update lastNotifiedAt and persist.
     * 4. Clear the cache after execution is completed.
     */
    // TODO: @Scheduled(cron = "0 5/30 * * * *")
    public void checkUpdatesAndSendNotifications() {
        log.info("Starting Vaccine Tracker Notification update...");
        ConcurrentHashMap<String, VaccineCenters> cache = new ConcurrentHashMap<>();
        final List<UserRequest> userRequests = userRequestManager.fetchAllUserRequests();
        userRequests.forEach(userRequest -> {
            final String lastNotifiedAt = userRequest.getLastNotifiedAt();
            if (userWasNotifiedRecently(lastNotifiedAt)) {
                log.info("Skipping sending notification to {} as they were notified already on {}", userRequest.getChatId(), lastNotifiedAt);
                return;
            }
            // process pin codes of each user
            userRequest.getPincodes().forEach(pincode -> {
                VaccineCenters vaccineCenters;
                if (cache.containsKey(pincode)) {
                    vaccineCenters = cache.get(pincode);
                } else {
                    // fetch from kafka store
                    vaccineCenters = vaccinePersistence.fetchVaccineCentersByPincode(pincode);
                    if (isNull(vaccineCenters) || vaccineCenters.centers.isEmpty()) {
                        log.info("No centers found for pin code {} in persistence store.", pincode);
                        return;
                    }
                }
                List<Center> eligibleCenters = eligibleVaccineCenters(userRequest.getChatId(), vaccineCenters);
                botService.notify(userRequest.getChatId(), eligibleCenters);
                cache.putIfAbsent(pincode, vaccineCenters); // update local cache
                userRequestManager.updateUserRequestLastNotifiedAt(userRequest, Utils.currentTime());
            });
        });
        cache.clear(); // clear cache
    }

    @Scheduled(cron = "0 0/15 * * * *")
    public void checkUpdatesDirectlyWithCowinAndSendNotifications() {
        log.info("Starting Vaccine Availability via Cowin API Notification...");
        AtomicInteger failedCowinApiCalls = new AtomicInteger(0);
        AtomicInteger notificationsSent = new AtomicInteger(0);

        ConcurrentHashMap<String, VaccineCenters> cache = new ConcurrentHashMap<>();
        final List<UserRequest> userRequests = userRequestManager.fetchAllUserRequests();
        userRequests.forEach(userRequest -> {
            /*final String lastNotifiedAt = userRequest.getLastNotifiedAt();
            if (userWasNotifiedRecently(lastNotifiedAt)) {
                log.info("Skipping sending notification to {} as they were notified already on {}", userRequest.getChatId(), lastNotifiedAt);
                return;
            }*/
            // process pin codes of each user
            userRequest.getPincodes().forEach(pincode -> {
                VaccineCenters vaccineCenters;
                if (cache.containsKey(pincode)) {
                    log.info("Found vaccine centers in local cache for pin code {}", pincode);
                    vaccineCenters = cache.get(pincode);
                } else {
                    // fetch from Cowin API
                    vaccineCenters = cowinApiClient.fetchCentersByPincode(pincode);
                    if (isNull(vaccineCenters) || vaccineCenters.centers.isEmpty()) {
                        if (isNull(vaccineCenters)) {
                            failedCowinApiCalls.incrementAndGet();
                        }
                        log.info("No centers found for pin code {} in persistence store.", pincode);
                        return;
                    }
                }
                cache.putIfAbsent(pincode, vaccineCenters); // update local cache
                introduceDelayEvery50ApiCalls(cache.size()); // to respect API rate limits
                List<Center> eligibleCenters = eligibleVaccineCenters(userRequest.getChatId(), vaccineCenters);
                if (eligibleCenters.isEmpty()) {
                    log.info("No eligible vaccine centers found for pin code {}", pincode);
                    return;
                }
                if (botService.notify(userRequest.getChatId(), eligibleCenters)) {
                    notificationsSent.incrementAndGet();
                    userRequestManager.updateUserRequestLastNotifiedAt(userRequest, Utils.currentTime());
                }
            });
        });
        botService.summary(cache.size(), failedCowinApiCalls, notificationsSent);
        cache.clear();
    }

    private void introduceDelayEvery50ApiCalls(int cacheSize) {
        if (cacheSize % 50 == 0) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // eat
            }
        }
    }

    List<Center> eligibleVaccineCenters(String userId, VaccineCenters vaccineCenters) {
        List<Center> eligibleCenters = new ArrayList<>();
        vaccineCenters.centers.forEach(center -> {
            List<Session> eligibleSessions = new ArrayList<>();
            center.sessions.forEach(session -> {
                if (usersOver45.contains(userId)) { // some users should be alerted for 45 too
                    if (vaccineCentersProcessor.ageLimit18AndAbove(session) && vaccineCentersProcessor.hasCapacity(session)) {
                        eligibleSessions.add(session);
                    }
                } else {
                    if (vaccineCentersProcessor.ageLimitExactly18(session) && vaccineCentersProcessor.hasCapacity(session)) {
                        eligibleSessions.add(session);
                    }
                }
            });
            if (!eligibleSessions.isEmpty()) {
                Center eligibleCenter = buildCenter(center);
                eligibleCenter.setSessions(eligibleSessions);
                eligibleCenters.add(eligibleCenter);
            }
        });
        return eligibleCenters;
    }

    private Center buildCenter(Center center) {
        return Center.builder()
                .centerId(center.getCenterId())
                .name(center.getName())
                .stateName(center.getStateName())
                .districtName(center.getDistrictName())
                .blockName(center.getBlockName())
                .pincode(center.getPincode())
                .feeType(center.getFeeType())
                .from(center.getFrom())
                .to(center.getTo())
                .latitude(center.getLatitude())
                .longitude(center.getLongitude())
                .build();
    }

    /*
     * Return true if user was notified within last 24 hours.
     */
    private boolean userWasNotifiedRecently(String lastNotifiedAt) {
        return nonNull(lastNotifiedAt) && !Utils.dayOld(lastNotifiedAt);
    }
}
