package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.availability.aws.CowinLambdaWrapper;
import org.covid19.vaccinetracker.notifications.bot.BotService;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.userrequests.model.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VaccineAvailabilityTest {
    @Mock
    private VaccinePersistence vaccinePersistence;

    @Mock
    private UserRequestManager userRequestManager;

    @Mock
    private BotService botService;

    @Mock
    private CowinLambdaWrapper cowinLambdaWrapper;

    @Mock
    private AvailabilityConfig config;
    // TODO: Add IT

    @Test
    public void testRefreshVaccineAvailabilityFromCowinViaLambda_happyScenario() {
        District aDistrict = new District(1, "Shahdara", new State(1, "Delhi"));
        when(userRequestManager.fetchAllUserDistricts()).thenReturn(singleton(aDistrict));

        AvailabilityStats availabilityStats = new AvailabilityStats();
        VaccineAvailability vaccineAvailability = new VaccineAvailability(vaccinePersistence,
                userRequestManager, availabilityStats, botService, cowinLambdaWrapper, config);
        vaccineAvailability.refreshVaccineAvailabilityFromCowinViaLambdaAsync();

        verify(cowinLambdaWrapper, times(1)).processDistrict(1);

        assertThat(availabilityStats.processedDistricts(), is(1));
    }

    // TODO: Add IT
    @Test
    public void testRefreshVaccineAvailabilityFromCowinViaLambda_nullCenters() {
        District aDistrict = new District(1, "Shahdara", new State(1, "Delhi"));
        when(userRequestManager.fetchAllUserDistricts()).thenReturn(singleton(aDistrict));

        AvailabilityStats availabilityStats = new AvailabilityStats();
        VaccineAvailability vaccineAvailability = new VaccineAvailability(vaccinePersistence,
                userRequestManager, availabilityStats, botService, cowinLambdaWrapper, config);
        vaccineAvailability.refreshVaccineAvailabilityFromCowinViaLambdaAsync();

        verify(cowinLambdaWrapper, times(1)).processDistrict(1);

        assertThat(availabilityStats.processedDistricts(), is(1));
    }
}
