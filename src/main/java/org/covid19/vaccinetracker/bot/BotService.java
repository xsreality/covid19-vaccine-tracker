package org.covid19.vaccinetracker.bot;

import org.covid19.vaccinetracker.model.VaccineCenters;

public interface BotService {
    void notify(String userId, VaccineCenters vaccineCenters);
}
