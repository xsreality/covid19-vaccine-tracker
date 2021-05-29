package org.covid19.vaccinetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Covid19VaccineTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(Covid19VaccineTrackerApplication.class, args);
    }
}
