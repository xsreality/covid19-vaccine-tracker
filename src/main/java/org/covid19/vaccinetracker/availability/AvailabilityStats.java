package org.covid19.vaccinetracker.availability;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AvailabilityStats {
    private final AtomicInteger processedPincodes = new AtomicInteger(0);
    private final AtomicInteger processedDistricts = new AtomicInteger(0);
    private final AtomicInteger failedApiCalls = new AtomicInteger(0);
    private final AtomicInteger unknownPincodes = new AtomicInteger(0);

    public void reset() {
        processedPincodes.set(0);
        processedDistricts.set(0);
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
    public void incrementUnknownPincodes() {
        unknownPincodes.incrementAndGet();
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
    public int unknownPincodes() {
        return unknownPincodes.get();
    }
}