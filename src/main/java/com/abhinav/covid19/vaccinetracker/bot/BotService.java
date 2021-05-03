package com.abhinav.covid19.vaccinetracker.bot;

import com.abhinav.covid19.vaccinetracker.model.VaccineCenters;

public interface BotService {
    String format(String userId, VaccineCenters vaccineCenters);

    void send(String message);
}
