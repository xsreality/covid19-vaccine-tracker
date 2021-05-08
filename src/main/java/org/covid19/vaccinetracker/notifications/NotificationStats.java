package org.covid19.vaccinetracker.notifications;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class NotificationStats {
    private final AtomicInteger userRequests = new AtomicInteger(0);
    private final AtomicInteger processedPincodes = new AtomicInteger(0);
    private final AtomicInteger failedApiCalls = new AtomicInteger(0);
    private final AtomicInteger notificationsSent = new AtomicInteger(0);

    public void reset() {
        userRequests.set(0);
        processedPincodes.set(0);
        failedApiCalls.set(0);
        notificationsSent.set(0);
    }

    public void incrementUserRequests() {
        userRequests.incrementAndGet();
    }

    public void incrementProcessedPincodes() {
        processedPincodes.incrementAndGet();
    }

    public void incrementfailedApiCalls() {
        failedApiCalls.incrementAndGet();
    }

    public void incrementNotificationsSent() {
        notificationsSent.incrementAndGet();
    }

    public int userRequests() {
        return userRequests.get();
    }

    public int processedPincodes() {
        return processedPincodes.get();
    }

    public int failedApiCalls() {
        return failedApiCalls.get();
    }

    public int notificationsSent() {
        return notificationsSent.get();
    }
}
