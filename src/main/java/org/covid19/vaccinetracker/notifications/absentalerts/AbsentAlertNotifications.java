package org.covid19.vaccinetracker.notifications.absentalerts;

import org.covid19.vaccinetracker.notifications.NotificationCache;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotificationId;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

/**
 * Sends summary notifications to active users who haven't received regular alerts in last 2 days.
 * It explains the reason why no alerts have been sent and optionally suggestions to increase
 * chances of receiving alerts.
 */
@Slf4j
@Component
public class AbsentAlertNotifications {
    private final UserRequestManager userRequestManager;
    private final NotificationCache cache;
    private final AbsentAlertAnalyzer analyzer;
    private final VaccinePersistence vaccinePersistence;

    public AbsentAlertNotifications(UserRequestManager userRequestManager, NotificationCache cache,
                                    AbsentAlertAnalyzer analyzer, VaccinePersistence vaccinePersistence) {
        this.userRequestManager = userRequestManager;
        this.cache = cache;
        this.analyzer = analyzer;
        this.vaccinePersistence = vaccinePersistence;
    }

    public Map<String, List<AbsentAlertCause>> onDemandAbsentAlertsNotification(String userId) {
        return Stream.of(userRequestManager.fetchUserRequest(userId))
                .filter(userRequest -> !userRequest.getPincodes().isEmpty())
                .flatMap(getLatestNotifications())
                .map(identifyCause())
                .collect(Collectors.groupingBy(AbsentAlertCause::getUserId))
                ;
    }

    @Scheduled(cron = "${jobs.cron.absentalerts.notifications:-}", zone = "IST")
    public void absentAlertsNotificationJob() {
        userRequestManager.fetchAllUserRequests()
                .stream()
                .filter(activeUsers())
                .flatMap(getLatestNotifications())
                .map(identifyCause())
                .collect(Collectors.groupingBy(AbsentAlertCause::getUserId))
                .forEach(sendNotification());
    }

    private Predicate<? super UserRequest> activeUsers() {
        return userRequest -> !userRequest.getPincodes().isEmpty();
    }

    private Function<UserRequest, Stream<AbsentAlertSource>> getLatestNotifications() {
        return userRequest -> userRequest.getPincodes()
                .stream()
                .map(pincode -> AbsentAlertSource.builder()
                        .userId(userRequest.getChatId())
                        .pincode(pincode)
                        .age(userRequest.getAge())
                        .dose(userRequest.getDose())
                        .vaccine(userRequest.getVaccine())
                        .latestNotification(
                                cache.userNotificationFor(new UserNotificationId(userRequest.getChatId(), pincode)).orElse(null))
                        .build());
    }

    private Function<AbsentAlertSource, AbsentAlertCause> identifyCause() {
        return source -> analyzer.analyze(source, vaccinePersistence.findAllSessionsByPincode(source.getPincode()));
    }

    private BiConsumer<String, List<AbsentAlertCause>> sendNotification() {
        return (pincode, causes) -> {
            log.info("pincode is {}", pincode);
            log.info("cause is {}", causes);
        };
    }
}
