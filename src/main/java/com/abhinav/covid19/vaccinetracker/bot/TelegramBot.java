package com.abhinav.covid19.vaccinetracker.bot;

import com.abhinav.covid19.vaccinetracker.model.VaccineCenters;

import org.springframework.stereotype.Service;

@Service
public class TelegramBot implements BotService {
    @Override
    public String format(String userId, VaccineCenters vaccineCenters) {
        return null;
    }

    @Override
    public void send(String message) {

    }
}
