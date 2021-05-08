package org.covid19.vaccinetracker.reconciliation;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ReconciliationStats {
    private final AtomicInteger unknownPincodes = new AtomicInteger(0);
    private final AtomicInteger failedReconciliations = new AtomicInteger(0);
    private final AtomicInteger failedWithUnknownDistrict = new AtomicInteger(0);
    private final AtomicInteger successfulReconciliations = new AtomicInteger(0);

    public void reset() {
        unknownPincodes.set(0);
        failedReconciliations.set(0);
        failedWithUnknownDistrict.set(0);
        successfulReconciliations.set(0);
    }

    public void incrementUnknownPincodes() {
        unknownPincodes.incrementAndGet();
    }

    public void incrementFailedReconciliations() {
        failedReconciliations.incrementAndGet();
    }

    public void incrementSuccessfulReconciliations() {
        successfulReconciliations.incrementAndGet();
    }

    public void incrementFailedWithUnknownDistrict() {
        failedWithUnknownDistrict.incrementAndGet();
    }

    public int unknownPincodes() {
        return unknownPincodes.get();
    }

    public int failedReconciliations() {
        return failedReconciliations.get();
    }

    public int failedWithUnknownDistrict() {
        return failedWithUnknownDistrict.get();
    }

    public int successfulReconciliations() {
        return successfulReconciliations.get();
    }
}
