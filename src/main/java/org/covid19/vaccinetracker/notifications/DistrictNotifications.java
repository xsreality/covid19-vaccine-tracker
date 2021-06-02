package org.covid19.vaccinetracker.notifications;

import org.covid19.vaccinetracker.notifications.bot.BotService;
import org.covid19.vaccinetracker.availability.cowin.CowinApiClient;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DistrictNotifications {
    private static final String MUMBAI_DISTRICT_CHANNEL_ID = "-1001318272903";
    private static final int MUMBAI_DISTRICT_ID = 395;
    private final BotService botService;
    private final VaccineCentersProcessor vaccineCentersProcessor;
    private final CowinApiClient cowinApiClient;

    public DistrictNotifications(BotService botService, VaccineCentersProcessor vaccineCentersProcessor, CowinApiClient cowinApiClient) {
        this.botService = botService;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.cowinApiClient = cowinApiClient;
    }

    @Scheduled(cron = "${jobs.cron.district.notifications:-}", zone = "IST")
    public void sendDistrictNotifications() {
        final VaccineCenters vaccineCenters = cowinApiClient.fetchSessionsByDistrict(MUMBAI_DISTRICT_ID);
        final List<Center> eligibleCenters = vaccineCentersProcessor.eligibleVaccineCenters(vaccineCenters, "999999");
        if (eligibleCenters.isEmpty()) {
            log.debug("No eligible vaccine centers found for district update");
            return;
        }
        if (botService.notifyAvailability(MUMBAI_DISTRICT_CHANNEL_ID, eligibleCenters)) {
            log.info("Sending update for Mumbai district on dedicated channel with {} eligible centers", eligibleCenters.size());
        }
    }
}
