package org.covid19.vaccinetracker.userrequests;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.covid19.vaccinetracker.persistence.kafka.KafkaStateStores;
import org.covid19.vaccinetracker.persistence.kafka.KafkaStreamsConfig;
import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.userrequests.model.State;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_18_44;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_45;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_BOTH;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_1;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_2;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_BOTH;
import static org.covid19.vaccinetracker.userrequests.model.Vaccine.ALL;
import static org.covid19.vaccinetracker.userrequests.model.Vaccine.COVAXIN;
import static org.covid19.vaccinetracker.userrequests.model.Vaccine.COVISHIELD;
import static org.covid19.vaccinetracker.userrequests.model.Vaccine.SPUTNIK_V;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.kafka.test.hamcrest.KafkaMatchers.hasValue;

@SpringBootTest(classes = {
        UserRequestProducerConfig.class,
        KafkaProperties.class,
        KafkaStateStores.class,
        KafkaStreamsConfig.class,
        UserRequestManager.class
})
@EmbeddedKafka(
        partitions = 1,
        topics = {"${topic.user.requests}", "${topic.user.districts}", "${topic.user.bypincode}"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@DirtiesContext
public class UserRequestManagerIT {
    @Value("${topic.user.requests}")
    private String userRequestsTopic;

    @SuppressWarnings("unused")
    @MockBean
    private MetadataStore metadataStore;

    @Autowired
    private UserRequestManager userRequestManager;

    @Autowired
    private KafkaTemplate<String, UserRequest> kafkaTemplate;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    public void testAcceptUserRequest() {
        userRequestManager.acceptUserRequest("123456", asList("440022", "411038"));
        final Consumer<String, String> consumer = buildConsumer("fetch.accept.user.requests.test");
        embeddedKafka.consumeFromEmbeddedTopics(consumer, userRequestsTopic);
        final ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
        AtomicBoolean recordFound = new AtomicBoolean(false);
        records.forEach(record -> {
            if ("123456".equals(record.key())) {
                assertThat(record, hasValue("{\"chatId\":\"123456\",\"pincodes\":[\"440022\",\"411038\"],\"districts\":[],\"age\":\"18-44\",\"dose\":\"Dose 1\",\"vaccine\":\"All\",\"lastNotifiedAt\":null}"));
                recordFound.set(true);
            }
        });
        assertTrue(recordFound.get());
    }

    @Test
    public void testFetchUserPincodes() throws Exception {
        kafkaTemplate.send(userRequestsTopic, "931543", new UserRequest("931543", asList("110045", "110081"), List.of(), null, null, null, null)).get();
        await().atMost(1, SECONDS).until(() -> userRequestManager.fetchAllUserRequests().size() >= 1);
        assertEquals(asList("110045", "110081"), userRequestManager.fetchUserPincodes("931543"));
    }

    @Test
    public void testUpdateUserRequestLastNotifiedAt() {
        userRequestManager.updateUserRequestLastNotifiedAt(
                new UserRequest("654321", asList("400026", "431122"), null, null, null, null, null),
                "2021-05-09T20:51:55.415207+05:30");
        final Consumer<String, String> consumer = buildConsumer("update.user.requests.lastNotifiedAt.test");
        embeddedKafka.consumeFromEmbeddedTopics(consumer, userRequestsTopic);
        final ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
        AtomicBoolean recordFound = new AtomicBoolean(false);
        records.forEach(record -> {
            if ("654321".equals(record.key())) {
                assertThat(record, hasValue("{\"chatId\":\"654321\",\"pincodes\":[\"400026\",\"431122\"],\"districts\":null,\"age\":null,\"dose\":null,\"vaccine\":null,\"lastNotifiedAt\":\"2021-05-09T20:51:55.415207+05:30\"}"));
                recordFound.set(true);
            }
        });
        assertTrue(recordFound.get());
    }

    @Test
    public void testFetchAllUserRequests() throws Exception {
        kafkaTemplate.send(userRequestsTopic, "456789", new UserRequest("456789", asList("360005", "110085"), null, null, null, null, null)).get();
        await().atMost(1, SECONDS).until(() -> userRequestManager.fetchAllUserRequests().size() >= 1);
        assertTrue(userRequestManager.fetchAllUserRequests()
                .stream()
                .anyMatch(userRequest -> "456789".equals(userRequest.getChatId())
                        && userRequest.getPincodes().containsAll(asList("360005", "110085"))
                        && isNull(userRequest.getLastNotifiedAt())));
    }

    @Test
    public void testFetchAllUserDistricts() throws Exception {
        District aDistrict = new District(1, "Shahdara", new State(1, "Delhi"));
        when(metadataStore.fetchDistrictsByPincode("110092")).thenReturn(singletonList(aDistrict));
        kafkaTemplate.send(userRequestsTopic, "999888", new UserRequest("999888", asList("110092", "110093"), null, null, null, null, null)).get();
        await().atMost(1, SECONDS).until(() -> userRequestManager.fetchAllUserDistricts().size() >= 1);
    }

    @Test
    public void testfetchAllUsersByPincode() throws Exception {
        // create user request and verify topology completed for usersByPincode
        kafkaTemplate.send(userRequestsTopic, "112233", new UserRequest("112233", List.of("751010"), null, null, null, null, null)).get();
        await().atMost(5, SECONDS).until(() -> nonNull(userRequestManager.fetchUsersByPincode("751010")));
        await().atMost(5, SECONDS).until(() -> extractUsers("751010").contains("112233"));

        // Add another user request for same pincode and and verify store updated for usersByPincode
        kafkaTemplate.send(userRequestsTopic, "112234", new UserRequest("112234", List.of("751010"), null, null, null, null, null)).get();
        await().atMost(5, SECONDS).until(() -> extractUsers("751010").containsAll(Set.of("112233", "112234")));

        // Update user request of 112233 from pincode 751010 to 847226
        kafkaTemplate.send(userRequestsTopic, "112233", new UserRequest("112233", List.of("847226"), null, null, null, null, null)).get();
        // verify store updated for pincode 847226
        await().atMost(5, SECONDS).until(() -> nonNull(userRequestManager.fetchUsersByPincode("847226")));
        await().atMost(5, SECONDS).until(() -> extractUsers("847226").contains("112233"));
        // verify store updated for pincode 751010
        await().atMost(5, SECONDS).until(() -> !extractUsers("751010").contains("112233"));

        // Terminate subscription of user 112233
        kafkaTemplate.send(userRequestsTopic, "112233", new UserRequest("112233", List.of(), null, null, null, null, null)).get();
        // verify store contains no references to user 112233
        await().atMost(5, SECONDS).until(() -> !extractUsers("847226").contains("112233"));
    }

    @Test
    public void testGetUserAgePreferenceFor18() throws Exception {
        final String userId = "user_with_age_pref_18";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("751010"), null, AGE_18_44.toString(), null, null, null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserAgePreference(userId), is(equalTo(AGE_18_44)));
    }

    @Test
    public void testGetUserAgePreferenceFor45() throws Exception {
        final String userId = "user_with_age_pref_45";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("751010"), null, AGE_45.toString(), null, null, null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserAgePreference(userId), is(equalTo(AGE_45)));
    }

    @Test
    public void testGetUserAgePreferenceForBoth() throws Exception {
        final String userId = "user_with_age_pref_both";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("751010"), null, AGE_BOTH.toString(), null, null, null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserAgePreference(userId), is(equalTo(AGE_BOTH)));
    }

    @Test
    public void testGetUserAgePreferenceWhenNotSet() throws Exception {
        String userId = "user_with_age_pref_not_set";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("751010"), null, null, null, null, null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserAgePreference(userId), is(equalTo(AGE_18_44)));
    }

    @Test
    public void testGetUserDosePreferenceForDose1() throws Exception {
        final String userId = "user_with_dose_pref_1";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("415002"), null, null, DOSE_1.toString(), null, null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserDosePreference(userId), is(equalTo(DOSE_1)));
    }

    @Test
    public void testGetUserDosePreferenceForDose2() throws Exception {
        final String userId = "user_with_dose_pref_2";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("415002"), null, null, DOSE_2.toString(), null, null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserDosePreference(userId), is(equalTo(DOSE_2)));
    }

    @Test
    public void testGetUserDosePreferenceForDoseBoth() throws Exception {
        final String userId = "user_with_dose_pref_both";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("415002"), null, null, DOSE_BOTH.toString(), null, null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserDosePreference(userId), is(equalTo(DOSE_BOTH)));
    }

    @Test
    public void testGetUserDosePreferenceForDoseNotSet() throws Exception {
        final String userId = "user_with_dose_pref_not_set";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("415002"), null, null, null, null, null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserDosePreference(userId), is(equalTo(DOSE_1)));
    }

    @Test
    public void testGetUserVaccinePreferenceForCovishield() throws Exception {
        final String userId = "user_with_vaccine_pref_covishield";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("415002"), null, null, null, COVISHIELD.toString(), null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserVaccinePreference(userId), is(equalTo(COVISHIELD)));
    }

    @Test
    public void testGetUserVaccinePreferenceForCovaxin() throws Exception {
        final String userId = "user_with_vaccine_pref_covaxin";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("415002"), null, null, null, COVAXIN.toString(), null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserVaccinePreference(userId), is(equalTo(COVAXIN)));
    }

    @Test
    public void testGetUserVaccinePreferenceForSputnikV() throws Exception {
        final String userId = "user_with_vaccine_pref_sputnikv";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("415002"), null, null, null, SPUTNIK_V.toString(), null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserVaccinePreference(userId), is(equalTo(SPUTNIK_V)));
    }

    @Test
    public void testGetUserVaccinePreferenceForVaccineNotSet() throws Exception {
        final String userId = "user_with_vaccine_pref_not_set";
        kafkaTemplate.send(userRequestsTopic, userId, new UserRequest(userId, List.of("415002"), null, null, null, ALL.toString(), null)).get();
        await().atMost(1, SECONDS).until(() -> nonNull(userRequestManager.fetchUserRequest(userId)));
        assertThat(userRequestManager.getUserVaccinePreference(userId), is(equalTo(ALL)));
    }

    private Set<String> extractUsers(String pincode) {
        return userRequestManager.fetchUsersByPincode(pincode).getUsers();
    }

    private <K, V> Consumer<K, V> buildConsumer(String groupId) {
        // Use the procedure documented at https://docs.spring.io/spring-kafka/reference/html/#embedded-kafka-annotation
        final Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupId, "false", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        final DefaultKafkaConsumerFactory<K, V> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        return consumerFactory.createConsumer();
    }
}
