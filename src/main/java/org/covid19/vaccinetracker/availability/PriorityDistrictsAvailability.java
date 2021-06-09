package org.covid19.vaccinetracker.availability;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.covid19.vaccinetracker.availability.aws.CowinLambdaWrapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

/**
 * Schedules availability check of priority districts
 */
@Slf4j
@Component
public class PriorityDistrictsAvailability {
    private final AvailabilityConfig config;
    private final CowinLambdaWrapper cowinLambdaWrapper;

    public PriorityDistrictsAvailability(AvailabilityConfig config, CowinLambdaWrapper cowinLambdaWrapper) {
        this.config = config;
        this.cowinLambdaWrapper = cowinLambdaWrapper;
    }

    @Scheduled(cron = "${jobs.cron.priority.districts.availability:-}", zone = "IST")
    public void refreshVaccineAvailabilityOfPriorityDistricts() {
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("prio-dist-%d").build())
                .submit(this::refreshPriorityDistrictsAvailabilityFromCowinViaLambdaAsync);
    }

    public void refreshPriorityDistrictsAvailabilityFromCowinViaLambdaAsync() {
        log.info("Refreshing Availability of Priority Districts via Lambda async");

        config.getPriorityDistricts()
                .parallelStream()
                .filter(Objects::nonNull)
                .mapToInt(Integer::valueOf)
                .boxed()
                .peek(district -> log.debug("processing priority district id {}", district))
                .forEach(cowinLambdaWrapper::processDistrict);

        log.info("Availability check of priority districts completed");
    }
}
