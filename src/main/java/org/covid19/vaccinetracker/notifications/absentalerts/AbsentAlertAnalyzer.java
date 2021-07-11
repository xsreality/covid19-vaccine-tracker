package org.covid19.vaccinetracker.notifications.absentalerts;

import org.covid19.vaccinetracker.model.CenterSession;
import org.covid19.vaccinetracker.userrequests.model.Age;
import org.covid19.vaccinetracker.userrequests.model.Vaccine;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Business logic ot analyze why a user has not received any alerts.
 */
@Component
public class AbsentAlertAnalyzer {
    public AbsentAlertCause analyze(AbsentAlertSource source, List<CenterSession> sessions) {
        AbsentAlertCause absentAlertCause = AbsentAlertCause.builder()
                .userId(source.getUserId())
                .pincode(source.getPincode())
                .build();

        List<CenterSession> relevantSessions = sessions
                .stream()
                .filter(relevantSessionPredicate(source))
                .collect(Collectors.toList());

        List<CenterSession> alternativeSessions = sessions
                .stream()
                .filter(relevantSessionPredicate(source).negate())
                .collect(Collectors.toList());

        // set last notified in cause
        ofNullable(source.getLatestNotification())
                .filter(userNotification -> dayOld(userNotification.getNotifiedAt()))
                .ifPresentOrElse(
                        userNotification -> absentAlertCause.setLastNotified(
                                String.format("You were last notified for the pincode %s on %s",
                                        source.getPincode(), Utils.convertToIST(userNotification.getNotifiedAt()))),
                        () -> absentAlertCause.setLastNotified(String.format("You have not received any notification for pincode %s", source.getPincode())));


        if (relevantSessions.isEmpty()) {
            // no session has opened for user pincode before, include a helpful message
            absentAlertCause.addCause(String.format("No centers found for pincode %s.", source.getPincode()));
        } else {
            // sessions have opened for user pincode before, include them in causes
            relevantSessions
                    .forEach(session -> {
                        String cause = String.format("%s (%s %s) last had open slots for %s on %s.",
                                session.getCenterName(), session.getDistrictName(),
                                session.getPincode(), session.getSessionVaccine(), session.getSessionDate());
                        absentAlertCause.addCause(cause);
                    });
        }
        // check if sessions have opened outside user preferences for same pincode
        // and include them in causes
        if (!alternativeSessions.isEmpty()) {
            alternativeSessions
                    .forEach(session -> {
                        String alternative = String.format("%s (%s %s) last had open slots for %s on %s.",
                                session.getCenterName(), session.getDistrictName(),
                                session.getPincode(), session.getSessionVaccine(), session.getSessionDate());
                        absentAlertCause.addAlternative(alternative);
                    });
        }
        return absentAlertCause;
    }

    private Predicate<CenterSession> relevantSessionPredicate(AbsentAlertSource source) {
        return centerSession ->
                matchingAgePreference(source.getAge(), centerSession.getMinAge())
                        && matchingVaccinePreference(source.getVaccine(), centerSession.getSessionVaccine());
    }

    private boolean matchingVaccinePreference(String userPrefVaccine, String sessionVaccine) {
        if (Vaccine.ALL.toString().equals(userPrefVaccine)) {
            return true;
        }
        return sessionVaccine.equalsIgnoreCase(userPrefVaccine);
    }

    private boolean dayOld(LocalDateTime notifiedAt) {
        return Duration.between(notifiedAt, LocalDateTime.now())
                .compareTo(Duration.ofHours(24)) >= 0;
    }

    private boolean matchingAgePreference(String userPrefAge, int sessionMinAge) {
        if (Age.AGE_18_44.toString().equals(userPrefAge)) {
            return sessionMinAge < 45;
        } else if (Age.AGE_45.toString().equals(userPrefAge)) {
            return sessionMinAge >= 45;
        } else {
            // age pref is both so just return true
            return true;
        }
    }
}
