package org.covid19.vaccinetracker.notifications.absentalerts;

import org.covid19.vaccinetracker.model.CenterSession;
import org.covid19.vaccinetracker.userrequests.model.Age;
import org.covid19.vaccinetracker.userrequests.model.Vaccine;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.reducing;

/**
 * Analyze why a user has not received any alerts.
 */
@Component
public class AbsentAlertAnalyzer {

    public AbsentAlertCause analyze(AbsentAlertSource source, List<CenterSession> sessions) {
        AbsentAlertCause absentAlertCause = AbsentAlertCause.builder()
                .userId(source.getUserId())
                .pincode(source.getPincode())
                .build();

        List<CenterSession> relevantSessions = sessions.stream().filter(relevantSessionPredicate(source)).collect(Collectors.toList());

        // set last notified in cause
        ofNullable(source.getLatestNotification())
                .filter(userNotification -> dayOld(userNotification.getNotifiedAt()))
                .ifPresentOrElse(
                        userNotification -> absentAlertCause.setLastNotified(
                                String.format("You were last notified for the pincode %s on %s",
                                        source.getPincode(), Utils.convertToIST(userNotification.getNotifiedAt()))),
                        () -> absentAlertCause.setLastNotified(
                                String.format("You have not received any notification for pincode %s", source.getPincode())));


        if (!relevantSessions.isEmpty()) {
            // sessions have opened for user pincode before, include them in causes
            relevantSessions
                    .stream()
                    .collect(groupingBy(CenterSession::getCenterName, maxBy(comparing(o -> toLocalDate(o.getSessionDate())))))
                    .values()
                    .stream()
                    .flatMap(Optional::stream)
                    .forEach(currentSession -> absentAlertCause.addCause(
                            String.format("%s (%s %s) last had open slots for %s on %s",
                                    currentSession.getCenterName(), currentSession.getDistrictName(),
                                    currentSession.getPincode(), currentSession.getSessionVaccine(), currentSession.getSessionDate())));
        } else {
            // no session has opened for user pincode before, include a helpful message
            absentAlertCause.addCause(String.format("No centers found for pincode %s.", source.getPincode()));
        }

        // identify recent 5 sessions
        sessions
                .stream()
                .collect(groupingBy(CenterSession::getCenterName, reducing((old, current) -> {
                    if (!old.getSessionDate().equalsIgnoreCase(current.getSessionDate())) {
                        current.setMultipleDates((nonNull(old.getMultipleDates()) ? old.getMultipleDates() : old.getSessionDate()) + ", " + current.getSessionDate());
                    }
                    return current;
                })))
                .values()
                .stream()
                .flatMap(Optional::stream)
                .sorted((o1, o2) -> toLocalDate(o2.getSessionDate()).compareTo(toLocalDate(o1.getSessionDate())))
                .limit(5)
                .forEach(session -> absentAlertCause.addRecent(
                        String.format("%s (%s %s) had availability of %s for %d+ on %s",
                                session.getCenterName(), session.getDistrictName(),
                                session.getPincode(), session.getSessionVaccine(), session.getMinAge(),
                                nonNull(session.getMultipleDates()) ? session.getMultipleDates() : session.getSessionDate())));
        return absentAlertCause;
    }

    private Predicate<CenterSession> relevantSessionPredicate(AbsentAlertSource source) {
        return centerSession ->
                matchingAgePreference(source.getAge(), centerSession.getMinAge())
                        && matchingVaccinePreference(source.getVaccine(), centerSession.getSessionVaccine());
    }

    private boolean matchingVaccinePreference(String userPrefVaccine, String sessionVaccine) {
        if (isNull(userPrefVaccine) || Vaccine.ALL.toString().equals(userPrefVaccine)) {
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
        } else if (isNull(userPrefAge)) {
            return sessionMinAge < 45;
        } else {
            // age pref is both so just return true
            return true;
        }
    }

    private LocalDate toLocalDate(String ddMMyyyy) {
        DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return LocalDate.parse(ddMMyyyy, inputFormat);
    }
}
