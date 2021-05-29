package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.availability.aws.CowinLambdaWrapper;
import org.covid19.vaccinetracker.notifications.bot.BotService;
import org.covid19.vaccinetracker.availability.cowin.CowinApiClient;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.notifications.VaccineCentersNotification;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.userrequests.model.State;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VaccineAvailabilityTest {
    @Mock
    private CowinApiClient cowinApiClient;

    @Mock
    private VaccinePersistence vaccinePersistence;

    @Mock
    private UserRequestManager userRequestManager;

    @Mock
    private VaccineCentersNotification notification;

    @Mock
    private BotService botService;

    @Mock
    private KafkaTemplate<String, String> updatedPincodesKafkaTemplate;

    @Mock
    private CowinLambdaWrapper cowinLambdaWrapper;

    @Test
    public void testRefreshVaccineAvailabilityFromCowinViaLambda_happyScenario() {
        District aDistrict = new District(1, "Shahdara", new State(1, "Delhi"));
        VaccineCenters vaccineCenters = createVaccineCenters();
        when(userRequestManager.fetchAllUserDistricts()).thenReturn(singleton(aDistrict));
        when(cowinLambdaWrapper.fetchSessionsByDistrict(1)).thenReturn(Stream.of(Optional.of(vaccineCenters)));

        AvailabilityStats availabilityStats = new AvailabilityStats();
        VaccineAvailability vaccineAvailability = new VaccineAvailability(cowinApiClient, vaccinePersistence,
                new VaccineCentersProcessor(vaccinePersistence, updatedPincodesKafkaTemplate), userRequestManager, availabilityStats,
                botService, cowinLambdaWrapper);
        vaccineAvailability.refreshVaccineAvailabilityFromCowinViaLambda();

        verify(vaccinePersistence, times(1)).persistVaccineCenters(vaccineCenters);
        verify(botService, times(1)).notifyOwner(anyString());

        assertThat(availabilityStats.processedDistricts(), is(1));
        assertThat(availabilityStats.totalApiCalls(), is(1));
    }

    @Test
    public void testRefreshVaccineAvailabilityFromCowinViaLambda_nullCenters() {
        District aDistrict = new District(1, "Shahdara", new State(1, "Delhi"));
        when(userRequestManager.fetchAllUserDistricts()).thenReturn(singleton(aDistrict));
        when(cowinLambdaWrapper.fetchSessionsByDistrict(1)).thenReturn(Stream.of(Optional.empty()));

        AvailabilityStats availabilityStats = new AvailabilityStats();
        VaccineAvailability vaccineAvailability = new VaccineAvailability(cowinApiClient, vaccinePersistence,
                new VaccineCentersProcessor(vaccinePersistence, updatedPincodesKafkaTemplate), userRequestManager, availabilityStats,
                botService, cowinLambdaWrapper);
        vaccineAvailability.refreshVaccineAvailabilityFromCowinViaLambda();

        verify(vaccinePersistence, times(0)).persistVaccineCenters(any());
        verify(botService, times(1)).notifyOwner(anyString());

        assertThat(availabilityStats.processedDistricts(), is(1));
        assertThat(availabilityStats.totalApiCalls(), is(1));
        assertThat(availabilityStats.failedApiCalls(), is(1));
    }

    @NotNull
    private VaccineCenters createVaccineCenters() {
        return new VaccineCenters(singletonList(
                Center.builder()
                        .centerId(123)
                        .name("RAJIV GANDHI SUPER SPECIALITY")
                        .pincode(110093)
                        .districtName("Shahdara")
                        .stateName("Delhi")
                        .sessions(singletonList(
                                Session.builder()
                                        .sessionId("abcd")
                                        .vaccine("COVISHIELD")
                                        .availableCapacity(5)
                                        .availableCapacityDose1(5) // as most people are looking for 1st dose, this will be used for alerting
                                        .availableCapacityDose2(0) // ignored for now
                                        .minAgeLimit(18)
                                        .date("15-05-2021")
                                        .build()))
                        .build()));
    }
}
