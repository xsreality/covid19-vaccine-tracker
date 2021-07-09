package org.covid19.vaccinetracker.notifications;

import org.covid19.vaccinetracker.availability.UpdatedPincodesProducerConfig;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.model.VaccineFee;
import org.covid19.vaccinetracker.notifications.bot.BotService;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.kafka.KafkaStateStores;
import org.covid19.vaccinetracker.persistence.kafka.KafkaStreamsConfig;
import org.covid19.vaccinetracker.userrequests.MetadataStore;
import org.covid19.vaccinetracker.userrequests.UserRequestProducerConfig;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = {
        UserRequestProducerConfig.class,
        UpdatedPincodesProducerConfig.class,
        KafkaProperties.class,
        KafkaStateStores.class,
        KafkaStreamsConfig.class,
        KafkaNotifications.class,
        NotificationStats.class
})
@EmbeddedKafka(
        partitions = 1,
        topics = {"${topic.user.requests}", "${topic.user.districts}", "${topic.user.bypincode}", "${topic.updated.pincodes}"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@DirtiesContext
public class KafkaNotificationsIT {
    @Value("${topic.user.requests}")
    private String userRequestsTopic;

    @Value("${topic.updated.pincodes}")
    private String updatedPincodesTopic;

    @MockBean
    private VaccinePersistence vaccinePersistence;

    @MockBean
    private VaccineCentersProcessor vaccineCentersProcessor;

    @SuppressWarnings("unused")
    @MockBean
    private MetadataStore metadataStore;

    @MockBean
    private NotificationCache cache;

    @MockBean
    private TelegramLambdaWrapper telegramLambdaWrapper;

    @SuppressWarnings("unused")
    @MockBean
    private BotService botService;

    @Autowired
    private NotificationStats stats;

    @Autowired
    private KafkaStateStores stateStores;

    @Autowired
    private KafkaTemplate<String, String> updatedPincodesKafkaTemplate;

    @Autowired
    private KafkaTemplate<String, UserRequest> userRequestKafkaTemplate;

    @BeforeEach
    public void setup() {
        userRequestKafkaTemplate.send(userRequestsTopic, "userA", new UserRequest("userA", List.of("110022"), null, null, null, null, null));
        userRequestKafkaTemplate.send(userRequestsTopic, "userB", new UserRequest("userB", List.of("110023"), null, null, null, null, null));
    }

    @Test
    public void testNotificationsStream_withEligibleCenters_notificationSent() {
        // allow usersByPincode stream to be updated from setup()
        await().atMost(2L, SECONDS).until(() -> nonNull(stateStores.usersByPincode("110022")));

        final VaccineCenters data = createCentersWithData();
        when(vaccinePersistence.fetchVaccineCentersByPincode("110022")).thenReturn(data);
        when(vaccineCentersProcessor.eligibleVaccineCenters(any(), anyString())).thenReturn(data.getCenters());
        when(cache.isNewNotification(anyString(), anyString(), any())).thenReturn(true);

        updatedPincodesKafkaTemplate.send(updatedPincodesTopic, "110022", "110022");

        await().atMost(2L, SECONDS).until(() -> stats.notificationsSent() >= 1);

        verify(telegramLambdaWrapper, times(1)).sendTelegramNotification(anyString(), anyString());
        verify(cache, times(1)).updateUser(anyString(), anyString(), any());
        verify(vaccinePersistence, times(1)).markProcessed(data);
    }

    @Test
    public void testNotificationsStream_withoutEligibleCenters_notificationNotSent() {
        // allow usersByPincode stream to be updated from setup()
        await().atMost(2L, SECONDS).until(() -> nonNull(stateStores.usersByPincode("110023")));

        final VaccineCenters data = createCentersWithoutData();
        when(vaccinePersistence.fetchVaccineCentersByPincode("110023")).thenReturn(data);
        when(cache.isNewNotification(anyString(), anyString(), any())).thenReturn(true);

        updatedPincodesKafkaTemplate.send(updatedPincodesTopic, "110023", "110023");

        verify(telegramLambdaWrapper, times(0)).sendTelegramNotification(anyString(), anyString());
        verify(cache, times(0)).updateUser(anyString(), anyString(), any());
        verify(vaccinePersistence, times(0)).markProcessed(data);
    }

    private VaccineCenters createCentersWithData() {
        return new VaccineCenters(List.of(
                Center.builder()
                        .centerId(123)
                        .name("RAJIV GANDHI SUPER SPECIALITY")
                        .pincode(110022)
                        .districtName("Shahdara")
                        .stateName("Delhi")
                        .sessions(singletonList(
                                Session.builder()
                                        .sessionId("abcd")
                                        .vaccine("COVISHIELD")
                                        .availableCapacity(5)
                                        .availableCapacityDose1(5)
                                        .availableCapacityDose2(0)
                                        .minAgeLimit(18)
                                        .date("15-05-2021")
                                        .build()))
                        .vaccineFees(List.of(
                                VaccineFee.builder()
                                        .vaccine("COVISHIELD")
                                        .fee("780")
                                        .build()
                        ))
                        .build()));
    }

    private VaccineCenters createCentersWithoutData() {
        return new VaccineCenters(List.of(
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
                                        .availableCapacity(0)
                                        .availableCapacityDose1(0)
                                        .availableCapacityDose2(0)
                                        .minAgeLimit(18)
                                        .date("15-05-2021")
                                        .build()))
                        .build()));
    }
}
