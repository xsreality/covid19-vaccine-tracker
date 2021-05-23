package org.covid19.vaccinetracker.userrequests;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.kafka.KafkaStateStores;
import org.covid19.vaccinetracker.persistence.kafka.KafkaStreamsConfig;
import org.covid19.vaccinetracker.persistence.mariadb.entity.District;
import org.covid19.vaccinetracker.persistence.mariadb.entity.State;
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
    private VaccinePersistence vaccinePersistence;

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
                assertThat(record, hasValue("{\"chatId\":\"123456\",\"pincodes\":[\"440022\",\"411038\"],\"lastNotifiedAt\":null}"));
                recordFound.set(true);
            }
        });
        assertTrue(recordFound.get());
    }

    @Test
    public void testFetchUserPincodes() throws Exception {
        kafkaTemplate.send(userRequestsTopic, "931543", new UserRequest("931543", asList("110045", "110081"), null)).get();
        await().atMost(1, SECONDS).until(() -> userRequestManager.fetchAllUserRequests().size() >= 1);
        assertEquals(asList("110045", "110081"), userRequestManager.fetchUserPincodes("931543"));
    }

    @Test
    public void testUpdateUserRequestLastNotifiedAt() {
        userRequestManager.updateUserRequestLastNotifiedAt(
                new UserRequest("654321", asList("400026", "431122"), null),
                "2021-05-09T20:51:55.415207+05:30");
        final Consumer<String, String> consumer = buildConsumer("update.user.requests.lastNotifiedAt.test");
        embeddedKafka.consumeFromEmbeddedTopics(consumer, userRequestsTopic);
        final ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
        AtomicBoolean recordFound = new AtomicBoolean(false);
        records.forEach(record -> {
            if ("654321".equals(record.key())) {
                assertThat(record, hasValue("{\"chatId\":\"654321\",\"pincodes\":[\"400026\",\"431122\"],\"lastNotifiedAt\":\"2021-05-09T20:51:55.415207+05:30\"}"));
                recordFound.set(true);
            }
        });
        assertTrue(recordFound.get());
    }

    @Test
    public void testFetchAllUserRequests() throws Exception {
        kafkaTemplate.send(userRequestsTopic, "456789", new UserRequest("456789", asList("360005", "110085"), null)).get();
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
        when(vaccinePersistence.fetchDistrictsByPincode("110092")).thenReturn(singletonList(aDistrict));
        kafkaTemplate.send(userRequestsTopic, "999888", new UserRequest("999888", asList("110092", "110093"), null)).get();
        await().atMost(1, SECONDS).until(() -> userRequestManager.fetchAllUserDistricts().size() >= 1);
    }

    @Test
    public void testfetchAllUsersByPincode() throws Exception {
        // create user request and verify topology completed for usersByPincode
        kafkaTemplate.send(userRequestsTopic, "112233", new UserRequest("112233", List.of("751010"), null)).get();
        await().atMost(5, SECONDS).until(() -> nonNull(userRequestManager.fetchUsersByPincode("751010")));
        await().atMost(5, SECONDS).until(() -> userRequestManager.fetchUsersByPincode("751010").getUsers().contains("112233"));

        // Add another user request for same pincode and and verify store updated for usersByPincode
        kafkaTemplate.send(userRequestsTopic, "112234", new UserRequest("112234", List.of("751010"), null)).get();
        await().atMost(5, SECONDS).until(() -> userRequestManager.fetchUsersByPincode("751010").getUsers().containsAll(Set.of("112233", "112234")));

        // Update user request of 112233 from pincode 751010 to 847226
        kafkaTemplate.send(userRequestsTopic, "112233", new UserRequest("112233", List.of("847226"), null)).get();
        // verify store updated for pincode 847226
        await().atMost(5, SECONDS).until(() -> nonNull(userRequestManager.fetchUsersByPincode("847226")));
        await().atMost(5, SECONDS).until(() -> userRequestManager.fetchUsersByPincode("847226").getUsers().contains("112233"));
        // verify store updated for pincode 751010
        await().atMost(5, SECONDS).until(() -> !userRequestManager.fetchUsersByPincode("751010").getUsers().contains("112233"));

        // Terminate subscription of user 112233
        kafkaTemplate.send(userRequestsTopic, "112233", new UserRequest("112233", List.of(), null)).get();
        // verify store contains no references to user 112233
        await().atMost(5, SECONDS).until(() -> !userRequestManager.fetchUsersByPincode("847226").getUsers().contains("112233"));
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
