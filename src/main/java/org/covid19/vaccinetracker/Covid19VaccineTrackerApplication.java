package org.covid19.vaccinetracker;

import org.covid19.vaccinetracker.bot.TelegramBot;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.abilitybots.api.db.MapDBContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;

@EnableScheduling
@SpringBootApplication
public class Covid19VaccineTrackerApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(Covid19VaccineTrackerApplication.class, args);
    }

    @Override
    public void run(String... args) {
    }

}
