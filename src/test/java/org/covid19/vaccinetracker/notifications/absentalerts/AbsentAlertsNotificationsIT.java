package org.covid19.vaccinetracker.notifications.absentalerts;

import org.covid19.vaccinetracker.model.CenterSession;
import org.covid19.vaccinetracker.notifications.NotificationCache;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotification;
import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotificationId;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = {
        AbsentAlertNotifications.class,
        AbsentAlertAnalyzer.class
})
@DirtiesContext
public class AbsentAlertsNotificationsIT {
    @MockBean
    private UserRequestManager userRequestManager;

    @MockBean
    private NotificationCache cache;

    @MockBean
    private VaccinePersistence persistence;

    @Autowired
    private AbsentAlertNotifications notifications;

    @Test
    public void testAbsentAlertsNotificationJob() {
        when(userRequestManager.fetchAllUserRequests()).thenReturn(userRequests());
        when(cache.userNotificationFor(UserNotificationId.builder().userId("9876").pincode("412308").build()))
                .thenReturn(Optional.of(UserNotification.builder()
                        .userNotificationId(UserNotificationId.builder().userId("9876").pincode("412308").build())
                        .notifiedAt(LocalDateTime.now().minusDays(2L))
                        .build()));
        when(persistence.findAllSessionsByPincode("412308")).thenReturn(sessions());
        notifications.absentAlertsNotificationJob();
        // verify notifications are sent
    }

    private List<CenterSession> sessions() {
        return List.of(
                CenterSession.builder()
                        .centerName("PMC G Fursungi Dispensary").districtName("Pune").pincode("412308")
                        .minAge(18).sessionDate("12-07-2021").sessionVaccine("COVISHIELD")
                        .build(),
                CenterSession.builder()
                        .centerName("PMC G Uruli Devachi Dispensary").districtName("Pune").pincode("412308")
                        .minAge(18).sessionDate("12-07-2021").sessionVaccine("COVISHIELD")
                        .build(),
                CenterSession.builder()
                        .centerName("PMC G NEW ENGLISH SCHOOL").districtName("Pune").pincode("412308")
                        .minAge(18).sessionDate("10-07-2021").sessionVaccine("COVISHIELD")
                        .build()
        );
    }

    private List<UserRequest> userRequests() {
        return List.of(new UserRequest("9876", List.of("412308"), List.of(), "18-44", "Dose 1", "Covishield", null));
    }
}
