package org.covid19.vaccinetracker.availability.aws;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.SessionEntity;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(MockitoExtension.class)
public class CowinLambdaWrapperTest {
    @Mock
    private AWSConfig awsConfig;
    @Mock
    private AWSLambda awsLambda;
    @Mock
    private AWSLambdaAsync awsLambdaAsync;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private VaccinePersistence vaccinePersistence;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void testFreshAvailabilityWithFreshSlots() {
        CowinLambdaWrapper lambdaWrapper = new CowinLambdaWrapper(awsConfig, awsLambda, awsLambdaAsync,
                objectMapper, vaccinePersistence, kafkaTemplate);
        Mockito.when(vaccinePersistence.findExistingSession(1205L, "22-05-2021", 18, "COVAXIN"))
                .thenReturn(Optional.of(SessionEntity.builder()
                        .id("32bbb37e-7cb4-4942-bd92-ac56d86490f9")
                        .vaccine("COVAXIN")
                        .availableCapacity(10)
                        .availableCapacityDose1(10)
                        .availableCapacityDose2(0)
                        .minAgeLimit(18)
                        .build()));
        final VaccineCenters actual = lambdaWrapper.freshAvailability(buildVaccineCenters());
        assertThat(actual.getCenters().size(), is(1));
        assertThat(actual.getCenters().get(0).getSessions().size(), is(1));
        assertThat(actual.getCenters().get(0).getSessions().get(0).isShouldNotify(), is(true));
    }

    @Test
    public void testFreshAvailabilityNoFreshSlots() {
        CowinLambdaWrapper lambdaWrapper = new CowinLambdaWrapper(awsConfig, awsLambda, awsLambdaAsync,
                objectMapper, vaccinePersistence, kafkaTemplate);
        Mockito.when(vaccinePersistence.findExistingSession(1205L, "22-05-2021", 18, "COVAXIN"))
                .thenReturn(Optional.of(SessionEntity.builder()
                        .id("32bbb37e-7cb4-4942-bd92-ac56d86490f9")
                        .vaccine("COVAXIN")
                        .availableCapacity(30)
                        .availableCapacityDose1(30)
                        .availableCapacityDose2(0)
                        .minAgeLimit(18)
                        .build()));
        final VaccineCenters actual = lambdaWrapper.freshAvailability(buildVaccineCenters());
        assertThat(actual.getCenters().size(), is(1));
        assertThat(actual.getCenters().get(0).getSessions().size(), is(1));
        assertThat(actual.getCenters().get(0).getSessions().get(0).isShouldNotify(), is(false));
    }

    /*
     * Cancellation is when available capacity increases by 1 or 2 slots only.
     */
    @Test
    public void testFreshAvailabilityWithCancellations() {
        CowinLambdaWrapper lambdaWrapper = new CowinLambdaWrapper(awsConfig, awsLambda, awsLambdaAsync,
                objectMapper, vaccinePersistence, kafkaTemplate);
        Mockito.when(vaccinePersistence.findExistingSession(1205L, "22-05-2021", 18, "COVAXIN"))
                .thenReturn(Optional.of(SessionEntity.builder()
                        .id("32bbb37e-7cb4-4942-bd92-ac56d86490f9")
                        .vaccine("COVAXIN")
                        .availableCapacity(13)
                        .availableCapacityDose1(13)
                        .availableCapacityDose2(0)
                        .minAgeLimit(18)
                        .build()));
        final VaccineCenters actual = lambdaWrapper.freshAvailability(buildVaccineCenters());
        assertThat(actual.getCenters().size(), is(1));
        assertThat(actual.getCenters().get(0).getSessions().size(), is(1));
        assertThat(actual.getCenters().get(0).getSessions().get(0).isShouldNotify(), is(false));
    }

    @NotNull
    private VaccineCenters buildVaccineCenters() {
        final VaccineCenters vaccineCenters = new VaccineCenters();
        vaccineCenters.setCenters(
                singletonList(Center.builder()
                        .centerId(1205)
                        .name("Mohalla Clinic Peeragarhi PHC")
                        .stateName("Delhi")
                        .districtName("West Delhi")
                        .pincode(110056)
                        .feeType("Free")
                        .sessions(singletonList(Session.builder()
                                .sessionId("32bbb37e-7cb4-4942-bd92-ac56d86490f9")
                                .date("22-05-2021")
                                .availableCapacity(15)
                                .availableCapacityDose1(15)
                                .availableCapacityDose2(0)
                                .minAgeLimit(18)
                                .vaccine("COVAXIN")
                                .build()))
                        .build()));
        return vaccineCenters;
    }
}
