package com.abhinav.covid19.vaccinetracker.bot;

import com.abhinav.covid19.vaccinetracker.model.VaccineCenters;

public interface BotService {
    void notify(String userId, VaccineCenters vaccineCenters);
}
