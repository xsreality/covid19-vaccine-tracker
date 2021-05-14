package org.covid19.vaccinetracker.availability;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AvailabilityStats {
    private final AtomicInteger processedPincodes = new AtomicInteger(0);
    private final AtomicInteger processedDistricts = new AtomicInteger(0);
    private final AtomicInteger totalApiCalls = new AtomicInteger(0);
    private final AtomicInteger failedApiCalls = new AtomicInteger(0);
    private final AtomicInteger unknownPincodes = new AtomicInteger(0);
    private Instant startTime;
    private Instant endTime;

    public void reset() {
        processedPincodes.set(0);
        processedDistricts.set(0);
        totalApiCalls.set(0);
        failedApiCalls.set(0);
        unknownPincodes.set(0);
    }

    public void incrementProcessedPincodes() {
        processedPincodes.incrementAndGet();
    }

    public void incrementProcessedDistricts() {
        processedDistricts.incrementAndGet();
    }

    public void incrementFailedApiCalls() {
        failedApiCalls.incrementAndGet();
    }

    public void incrementTotalApiCalls() {
        totalApiCalls.incrementAndGet();
    }

    public void incrementUnknownPincodes() {
        unknownPincodes.incrementAndGet();
    }

    public void noteStartTime() {
        startTime = Instant.now();
    }

    public void noteEndTime() {
        endTime = Instant.now();
    }

    public int processedPincodes() {
        return processedPincodes.get();
    }

    public int processedDistricts() {
        return processedDistricts.get();
    }

    public int failedApiCalls() {
        return failedApiCalls.get();
    }

    public int totalApiCalls() {
        return totalApiCalls.get();
    }

    public int unknownPincodes() {
        return unknownPincodes.get();
    }

    public String timeTaken() {
        return Duration.between(startTime, endTime).toString();
    }
}
