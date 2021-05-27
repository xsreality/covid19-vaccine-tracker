package org.covid19.vaccinetracker.notifications;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class NotificationStats {
    private final AtomicInteger userRequests = new AtomicInteger(0);
    private final AtomicInteger processedPincodes = new AtomicInteger(0);
    private final AtomicInteger failedApiCalls = new AtomicInteger(0);
    private final AtomicInteger notificationsSent = new AtomicInteger(0);
    private final AtomicInteger notificationsErrors = new AtomicInteger(0);
    private Instant startTime;
    private Instant endTime;

    public void reset() {
        userRequests.set(0);
        processedPincodes.set(0);
        failedApiCalls.set(0);
        notificationsSent.set(0);
        notificationsErrors.set(0);
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

    public void incrementNotificationsErrors() {
        notificationsErrors.incrementAndGet();
    }

    public void noteStartTime() {
        startTime = Instant.now();
    }

    public void noteEndTime() {
        endTime = Instant.now();
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

    public int notificationsErrors() {
        return notificationsErrors.get();
    }

    public String timeTaken() {
        return Duration.between(startTime, endTime).toString();
    }
}
