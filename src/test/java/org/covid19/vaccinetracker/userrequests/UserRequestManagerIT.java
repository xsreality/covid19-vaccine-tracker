package org.covid19.vaccinetracker.userrequests;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.persistence.kafka.KafkaStateStores;
import org.covid19.vaccinetracker.persistence.kafka.KafkaStreamsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        topics = {"${topic.user.requests}"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@DirtiesContext
public class UserRequestManagerIT {
    @Value("${topic.user.requests}")
    private String userRequestsTopic;

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
        final List<UserRequest> userRequests = userRequestManager.fetchAllUserRequests();
        assertThat(userRequests.size(), is(greaterThanOrEqualTo(1)));
        assertTrue(userRequests
                .stream()
                .anyMatch(
                        userRequest -> "456789".equals(userRequest.getChatId())
                                && userRequest.getPincodes().containsAll(asList("360005", "110085"))
                                && isNull(userRequest.getLastNotifiedAt())));
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
