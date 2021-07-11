package org.covid19.vaccinetracker.notifications.absentalerts;

import org.covid19.vaccinetracker.model.CenterSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

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