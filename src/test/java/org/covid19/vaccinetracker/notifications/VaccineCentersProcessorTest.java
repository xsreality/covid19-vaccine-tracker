package org.covid19.vaccinetracker.notifications;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.userrequests.model.Vaccine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_18_44;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_45;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_BOTH;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_1;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_2;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_BOTH;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VaccineCentersProcessorTest {
    private VaccineCentersProcessor processor;

    @Mock
    private UserRequestManager userRequestManager;

    @BeforeEach
    public void setup() {
        processor = new VaccineCentersProcessor(userRequestManager);
    }

    @Test
    public void testHasCapacity() {
        //
        assertTrue(processor.hasCapacity(Session.builder()
                .availableCapacity(5)
                .availableCapacityDose1(5)
                .availableCapacityDose2(0)
                .build()), "True if AC=dose1+dose2");
        assertTrue(processor.hasCapacity(Session.builder()
                .availableCapacity(8)
                .availableCapacityDose1(5)
                .availableCapacityDose2(3)
                .build()), "True if AC=dose1+dose2");
        assertFalse(processor.hasCapacity(Session.builder()
                .availableCapacity(8)
                .availableCapacityDose1(0)
                .availableCapacityDose2(0)
                .build()), "False if dose1 and dose2 are zero");
        assertFalse(processor.hasCapacity(Session.builder()
                .availableCapacity(8)
                .availableCapacityDose1(0)
                .availableCapacityDose2(8)
                .build()), "False if dose1 is zero (ignore dose2)");
    }

    @Test
    public void testEligibleVaccineCenters_WhenNullCenters_ReturnEmpty() {
        VaccineCenters vaccineCenters = new VaccineCenters(null);
        assertThat(processor.eligibleVaccineCenters(vaccineCenters, "123"), is(List.of()));
    }

    @Test
    public void testEligibleVaccineCenters_WhenValidCentersFor18_44_NoUserPrefs() {
        when(userRequestManager.getUserAgePreference("user_who_wants_18_alerts")).thenReturn(AGE_18_44); // default
        when(userRequestManager.getUserDosePreference("user_who_wants_18_alerts")).thenReturn(DOSE_1); // default
        when(userRequestManager.getUserVaccinePreference("user_who_wants_18_alerts")).thenReturn(Vaccine.ALL); // default
        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "user_who_wants_18_alerts");
        assertThat(actual, is(not(emptyList())));
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getSessions().size(), is(1));
        assertThat(actual.get(0).getSessions().get(0).getSessionId(), is(equalTo("session_for_18_dose1")));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_Age18_Dose1_Covishield() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_18_44);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_1);
        when(userRequestManager.getUserVaccinePreference("123")).thenReturn(Vaccine.COVISHIELD);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual, is(not(emptyList())));
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getSessions().size(), is(1));
        assertThat(actual.get(0).getSessions().get(0).getSessionId(), is(equalTo("session_for_18_dose1")));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_Age18_Dose1_Covaxin() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_18_44);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_1);
        when(userRequestManager.getUserVaccinePreference("123")).thenReturn(Vaccine.COVAXIN);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual, is(emptyList()));
        assertThat(actual.size(), is(0));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_Age18_Dose1_SputnikV() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_18_44);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_1);
        when(userRequestManager.getUserVaccinePreference("123")).thenReturn(Vaccine.SPUTNIK_V);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual, is(emptyList()));
        assertThat(actual.size(), is(0));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_Age18_Dose1_AnyVaccine() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_18_44);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_1);
        when(userRequestManager.getUserVaccinePreference("123")).thenReturn(Vaccine.ALL);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual, is(not(emptyList())));
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getSessions().size(), is(1));
        assertThat(actual.get(0).getSessions().get(0).getSessionId(), is(equalTo("session_for_18_dose1")));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_Age18_Dose2() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_18_44);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_2);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual, is(emptyList()));
        assertThat(actual.size(), is(0));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_Age18_DoseBoth() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_18_44);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_BOTH);
        when(userRequestManager.getUserVaccinePreference("123")).thenReturn(Vaccine.ALL);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getSessions().size(), is(1));
        assertThat(actual.get(0).getSessions().get(0).getSessionId(), is(equalTo("session_for_18_dose1")));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_Age45_Dose1() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_45);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_1);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual, is(emptyList()));
        assertThat(actual.size(), is(0));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_Age45_Dose2() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_45);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_2);
        when(userRequestManager.getUserVaccinePreference("123")).thenReturn(Vaccine.ALL);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual, is(not(emptyList())));
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getSessions().size(), is(1));
        assertThat(actual.get(0).getSessions().get(0).getSessionId(), is(equalTo("session_for_45_dose2")));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_Age45_DoseBoth() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_45);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_BOTH);
        when(userRequestManager.getUserVaccinePreference("123")).thenReturn(Vaccine.ALL);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual, is(not(emptyList())));
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getSessions().size(), is(1));
        assertThat(actual.get(0).getSessions().get(0).getSessionId(), is(equalTo("session_for_45_dose2")));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_AgeBoth_Dose1() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_BOTH);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_1);
        when(userRequestManager.getUserVaccinePreference("123")).thenReturn(Vaccine.ALL);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual, is(not(emptyList())));
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getSessions().size(), is(1));
        assertTrue(actual.get(0).getSessions().stream().anyMatch(session -> "session_for_18_dose1".equals(session.getSessionId())));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_AgeBoth_Dose2() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_BOTH);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_2);
        when(userRequestManager.getUserVaccinePreference("123")).thenReturn(Vaccine.ALL);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual, is(not(emptyList())));
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getSessions().size(), is(1));
        assertTrue(actual.get(0).getSessions().stream().anyMatch(session -> "session_for_45_dose2".equals(session.getSessionId())));
    }

    @Test
    public void testEligibleVaccineCenters_WhenPreference_AgeBoth_DoseBoth_AnyVaccine() {
        when(userRequestManager.getUserAgePreference("123")).thenReturn(AGE_BOTH);
        when(userRequestManager.getUserDosePreference("123")).thenReturn(DOSE_BOTH);
        when(userRequestManager.getUserVaccinePreference("123")).thenReturn(Vaccine.ALL);

        VaccineCenters vaccineCenters = createCentersWithData();
        List<Center> actual = processor.eligibleVaccineCenters(vaccineCenters, "123");
        assertThat(actual, is(not(emptyList())));
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getSessions().size(), is(2));
        assertTrue(actual.get(0).getSessions().stream().anyMatch(session -> "session_for_18_dose1".equals(session.getSessionId())));
        assertTrue(actual.get(0).getSessions().stream().anyMatch(session -> "session_for_45_dose2".equals(session.getSessionId())));
    }

    private VaccineCenters createCentersWithData() {
        return new VaccineCenters(List.of(
                Center.builder()
                        .centerId(12345)
                        .name("RAJIV GANDHI SUPER SPECIALITY")
                        .pincode(110022)
                        .districtName("Shahdara")
                        .stateName("Delhi")
                        .sessions(List.of(
                                Session.builder()
                                        .sessionId("session_for_18_dose1")
                                        .vaccine("COVISHIELD")
                                        .availableCapacity(75)
                                        .availableCapacityDose1(75)
                                        .availableCapacityDose2(0)
                                        .minAgeLimit(18)
                                        .date("15-05-2021")
                                        .build(),
                                Session.builder()
                                        .sessionId("session_for_45_dose2")
                                        .vaccine("COVAXIN")
                                        .availableCapacity(65)
                                        .availableCapacityDose1(0)
                                        .availableCapacityDose2(65)
                                        .minAgeLimit(45)
                                        .date("15-05-2021")
                                        .build()))
                        .build()));
    }
}
