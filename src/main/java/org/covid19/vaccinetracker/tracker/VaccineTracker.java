package org.covid19.vaccinetracker.tracker;

import org.covid19.vaccinetracker.bot.BotService;
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
    private final BotService botService;

    public VaccineTracker(CowinApiClient cowinApiClient, VaccinePersistence vaccinePersistence,
                          VaccineCentersProcessor vaccineCentersProcessor, BotService botService) {
        this.cowinApiClient = cowinApiClient;
        this.vaccinePersistence = vaccinePersistence;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.botService = botService;
    }

    // TODO: Convert to scheduler
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

    // TODO: Add logic to read from user requests store and send Bot notifications
    // TODO: Convert to scheduler
    public VaccineCenters fetchVaccineAvailability(String pincode) {
        /*
         * 1. For each user, if vaccine centers exist and last notified
         * is beyond 24 hours, send bot message.
         * 2. Cache the bot message in a local cache temporarily to
         * send same alert faster for other users with same pin code.
         * 3. Update lastNotifiedAt and persist.
         * 4. Clear the cache after execution is completed.
         */
        log.info("Fetching Vaccine availability from local store");
        // botservice.notify(...)
        return vaccinePersistence.fetchVaccineCentersByPincode(pincode);
    }
}
