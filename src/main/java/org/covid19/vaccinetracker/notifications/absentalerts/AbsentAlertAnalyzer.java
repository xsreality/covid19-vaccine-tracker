package org.covid19.vaccinetracker.notifications.absentalerts;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * Business logic ot analyze why a user has not received any alerts.
 */
@Component
public class AbsentAlertAnalyzer {

    public AbsentAlertCause analyze(AbsentAlertSource source) {
        AbsentAlertCause cause = AbsentAlertCause.builder()
                .userId(source.getUserId())
                .pincode(source.getPincode())
                .build();

        return ofNullable(source.getLatestNotification())
                .filter(userNotification -> dayOld(userNotification.getNotifiedAt()))
                .map(userNotification -> {
                    cause.setCauses(List.of());
                    return cause;
                })
                .orElseGet(() -> {
                    // user was never notified for this pincode.
                    cause.setCauses(List.of());
                    return cause;
                });
    }

    private boolean dayOld(LocalDateTime notifiedAt) {
        return Duration.between(notifiedAt, LocalDateTime.now())
                .compareTo(Duration.ofHours(24)) >= 0;
    }
}
