package org.covid19.vaccinetracker.notifications.absentalerts;

import org.covid19.vaccinetracker.model.CenterSession;
import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotification;
import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotificationId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class AbsentAlertsAnalyzerTest {
    @Test
    public void verifyAnalyzeBehaviour() {
        List<CenterSession> sessions = createSessions();
        AbsentAlertSource source = AbsentAlertSource.builder()
                .userId("1234").pincode("700014").age("45+").vaccine("Covaxin").dose("Dose 2").build();
        AbsentAlertAnalyzer analyzer = new AbsentAlertAnalyzer();
        final AbsentAlertCause cause = analyzer.analyze(source, sessions);
        log.info("Cause is {}", cause.getCauses());
        log.info("Alternative is {}", cause.getRecents());
        assertTrue(cause.getRecents().contains("BELLE VUE CLINIC (Kolkata 700014) had availability of COVISHIELD for 45+ on 17-07-2021, 16-07-2021, 15-07-2021, 14-07-2021"));
        assertTrue(cause.getRecents().contains("CNMCH PPU COVAXIN (Kolkata 700014) had availability of COVAXIN for 18+ on 12-06-2021"));
        assertTrue(cause.getRecents().contains("NRS PPU MCH COVAXIN (Kolkata 700014) had availability of COVAXIN for 45+ on 12-06-2021"));
        assertTrue(cause.getRecents().contains("UPHC-60 (Kolkata 700014) had availability of COVAXIN for 45+ on 05-06-2021"));
    }

    @Test
    public void verifyAnalyzeBehaviourWithUnsetPreferences() {
        List<CenterSession> sessions = createSessions();
        AbsentAlertSource source = AbsentAlertSource.builder()
                .userId("1234").pincode("700014").age(null).vaccine(null).dose(null)
                .latestNotification(UserNotification.builder()
                        .userNotificationId(UserNotificationId.builder().userId("1234").pincode("700014").build())
                        .notifiedAt(LocalDateTime.now().minusDays(2L))
                        .build())
                .build();
        AbsentAlertAnalyzer analyzer = new AbsentAlertAnalyzer();
        final AbsentAlertCause cause = analyzer.analyze(source, sessions);
        assertFalse(cause.getCauses().contains("No centers found for pincode 700014"));
    }

    private List<CenterSession> createSessions() {
        return List.of(
                CenterSession.builder()
                        .centerName("NRS PPU MCH COVAXIN").districtName("Kolkata").pincode("700014")
                        .minAge(18).sessionDate("12-06-2021").sessionVaccine("COVAXIN")
                        .build(),
                CenterSession.builder()
                        .centerName("NRS PPU MCH COVAXIN").districtName("Kolkata").pincode("700014")
                        .minAge(45).sessionDate("12-06-2021").sessionVaccine("COVAXIN")
                        .build(),
                CenterSession.builder()
                        .centerName("UPHC-54").districtName("Kolkata").pincode("700014")
                        .minAge(45).sessionDate("05-06-2021").sessionVaccine("COVAXIN")
                        .build(),
                CenterSession.builder()
                        .centerName("UPHC-53").districtName("Kolkata").pincode("700014")
                        .minAge(45).sessionDate("05-06-2021").sessionVaccine("COVAXIN")
                        .build(),
                CenterSession.builder()
                        .centerName("UPHC-60").districtName("Kolkata").pincode("700014")
                        .minAge(45).sessionDate("05-06-2021").sessionVaccine("COVAXIN")
                        .build(),
                CenterSession.builder()
                        .centerName("BR Singh Railway Hosp COVAXIN").districtName("Kolkata").pincode("700014")
                        .minAge(45).sessionDate("12-06-2021").sessionVaccine("COVAXIN")
                        .build(),
                CenterSession.builder()
                        .centerName("BR Singh Railway Hosp COVAXIN").districtName("Kolkata").pincode("700014")
                        .minAge(18).sessionDate("12-06-2021").sessionVaccine("COVAXIN")
                        .build(),
                CenterSession.builder()
                        .centerName("CNMCH PPU COVAXIN").districtName("Kolkata").pincode("700014")
                        .minAge(45).sessionDate("12-06-2021").sessionVaccine("COVAXIN")
                        .build(),
                CenterSession.builder()
                        .centerName("CNMCH PPU COVAXIN").districtName("Kolkata").pincode("700014")
                        .minAge(18).sessionDate("12-06-2021").sessionVaccine("COVAXIN")
                        .build(),
                CenterSession.builder()
                        .centerName("BELLE VUE CLINIC").districtName("Kolkata").pincode("700014")
                        .minAge(45).sessionDate("17-07-2021").sessionVaccine("COVISHIELD")
                        .build(),
                CenterSession.builder()
                        .centerName("BELLE VUE CLINIC").districtName("Kolkata").pincode("700014")
                        .minAge(45).sessionDate("16-07-2021").sessionVaccine("COVISHIELD")
                        .build(),
                CenterSession.builder()
                        .centerName("BELLE VUE CLINIC").districtName("Kolkata").pincode("700014")
                        .minAge(45).sessionDate("15-07-2021").sessionVaccine("COVISHIELD")
                        .build(),
                CenterSession.builder()
                        .centerName("BELLE VUE CLINIC").districtName("Kolkata").pincode("700014")
                        .minAge(45).sessionDate("14-07-2021").sessionVaccine("COVISHIELD")
                        .build()
        );
    }
}
