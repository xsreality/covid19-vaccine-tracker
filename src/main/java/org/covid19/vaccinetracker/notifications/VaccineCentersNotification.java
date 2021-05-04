package org.covid19.vaccinetracker.notifications;

import org.covid19.vaccinetracker.bot.BotService;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Service
public class VaccineCentersNotification {
    private final BotService botService;
    private final UserRequestManager userRequestManager;
    private final VaccinePersistence vaccinePersistence;

    public VaccineCentersNotification(BotService botService, UserRequestManager userRequestManager,
                                      VaccinePersistence vaccinePersistence) {
        this.botService = botService;
        this.userRequestManager = userRequestManager;
        this.vaccinePersistence = vaccinePersistence;
    }

    /*
     * 1. For each user, if vaccine centers exist and last notified
     * is beyond 24 hours, send bot message.
     * 2. Cache the bot message in a local cache temporarily to
     * send same alert faster for other users with same pin code.
     * 3. Update lastNotifiedAt and persist.
     * 4. Clear the cache after execution is completed.
     */
    // TODO: Convert to scheduler
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
                botService.notify(userRequest.getChatId(), vaccineCenters);
                cache.putIfAbsent(pincode, vaccineCenters); // update local cache
                userRequestManager.updateUserRequestLastNotifiedAt(userRequest, Utils.currentTime());
            });
        });
        cache.clear(); // clear cache
    }

    /*
     * Return true if user was notified within last 24 hours.
     */
    private boolean userWasNotifiedRecently(String lastNotifiedAt) {
        return nonNull(lastNotifiedAt) && !Utils.dayOld(lastNotifiedAt);
    }
}
