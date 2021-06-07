package org.covid19.vaccinetracker.persistence.kafka;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.test.TestRecord;
import org.covid19.vaccinetracker.model.UsersByPincode;
import org.covid19.vaccinetracker.model.UsersByPincodeSerde;
import org.covid19.vaccinetracker.userrequests.MetadataStore;
import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.userrequests.model.Pincode;
import org.covid19.vaccinetracker.userrequests.model.State;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.covid19.vaccinetracker.userrequests.model.UserRequestSerde;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UsersByPincodeTransformerTest {
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    private static final String AGGREGRATE_STORE_NAME = "aggregate-store";

    private TopologyTestDriver driver;
    private TestInputTopic<String, UserRequest> inputTopic;
    private TestOutputTopic<String, UsersByPincode> outputTopic;
    private KeyValueStore<Object, Object> store;

    @Mock
    private MetadataStore metadataStore;

    @BeforeEach
    public void setup() {
        UsersByPincodeTransformer transformer = new UsersByPincodeTransformer(AGGREGRATE_STORE_NAME, metadataStore);
        StreamsBuilder builder = new StreamsBuilder();
        builder.addStateStore(
                Stores.keyValueStoreBuilder(
                        Stores.inMemoryKeyValueStore(AGGREGRATE_STORE_NAME),
                        Serdes.String(),
                        new UsersByPincodeSerde()).withLoggingDisabled());
        builder
                .stream(INPUT, Consumed.with(Serdes.String(), new UserRequestSerde()))
                .transform(() -> transformer, AGGREGRATE_STORE_NAME)
                .to(OUTPUT, Produced.with(Serdes.String(), new UsersByPincodeSerde()));

        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9999");

        driver = new TopologyTestDriver(builder.build(), config);

        inputTopic = driver.createInputTopic(INPUT, Serdes.String().serializer(),
                new UserRequestSerde().serializer());
        outputTopic = driver.createOutputTopic(OUTPUT, Serdes.String().deserializer(),
                new UsersByPincodeSerde().deserializer());

        // pre-populate store
        store = driver.getKeyValueStore(AGGREGRATE_STORE_NAME);
        store.put("411038", new UsersByPincode("411038", Set.of("1234")));
    }

    @AfterEach
    public void tearDown() {
        driver.close();
    }

    @Test
    public void shouldFlushStoreForFirstInput() {
        inputTopic.pipeInput("9999", new UserRequest("9999", List.of("110092"), null, null, null, null, null));
        final TestRecord<String, UsersByPincode> record = outputTopic.readRecord();
        assertThat(record.key(), is(equalTo("110092")));
        assertThat(record.value(), is(equalTo(new UsersByPincode("110092", Set.of("9999")))));
        assertThat(outputTopic.isEmpty(), is(true));
    }

    @Test
    public void shouldUpdateStoreForSecondUserWithSamePincode() {
        // User 1234 has pincode 411038 set in setup()
        // Now user 4567 sets same pincode 411038
        inputTopic.pipeInput("4567", new UserRequest("4567", List.of("411038"), null, null, null, null, null));

        // store should have both users for pincode 411038
        assertThat(store.get("411038"), is(equalTo(new UsersByPincode("411038", Set.of("1234", "4567")))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("411038", new UsersByPincode("411038", Set.of("1234", "4567"))))));
    }

    @Test
    public void shouldUpdateStoreWhenUserUpdatesPincode() {
        // User 1234 has pincode 411038 set in setup()
        // Now user 1234 updates pincode to 422104
        inputTopic.pipeInput("1234", new UserRequest("1234", List.of("422104"), null, null, null, null, null));

        // store should have user 1234 for pincode 422104
        assertThat(store.get("422104"), is(equalTo(new UsersByPincode("422104", Set.of("1234")))));
        // store should remove user 1234 for pincode 411038
        assertThat(store.get("411038"), is(equalTo(new UsersByPincode("411038", Set.of()))));
    }

    @Test
    public void shouldUpdateStoreWhenUserRemovesAllPincodes() {
        // User 1234 has pincode 411038 set in setup()
        // Now user 1234 stops subscription (empty pincodes)
        inputTopic.pipeInput("1234", new UserRequest("1234", List.of(), null, null, null, null, null));

        // store should have no users for pincode 411038
        assertThat(store.get("411038"), is(equalTo(new UsersByPincode("411038", Set.of()))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("411038", new UsersByPincode("411038", Set.of())))));
    }

    @Test
    public void shouldUpdateStoreWhenUserSendsMultiplePincodes() {
        // User 4932 sets 2 pincodes 682026, 682027
        inputTopic.pipeInput("4932", new UserRequest("4932", List.of("682026", "682027"), null, null, null, null, null));

        // store should have user 4932 for both pincodes 682026, 682027
        assertThat(store.get("682027"), is(equalTo(new UsersByPincode("682027", Set.of("4932")))));
        assertThat(store.get("682026"), is(equalTo(new UsersByPincode("682026", Set.of("4932")))));
    }

    /* Test district */

    @Test
    public void shouldUpdateStoreWhenUserSendsDistrict() {
        // mock metadata store
        when(metadataStore.fetchPincodesByDistrictId(363)).thenReturn(List.of(
                Pincode.builder()
                        .district(new District(363, "Pune", new State(21, "Maharashtra")))
                        .pincode("411057").build()));

        // User 1234 has pincode 411038 set in setup()
        // Now user 1234 sets district 363
        inputTopic.pipeInput("1234", new UserRequest("1234", List.of("411038"), List.of(363), null, null, null, null));

        // store should have user 1234 for both pincodes 411038 (set directly) and 411057 (via district)
        assertThat(store.get("411038"), is(equalTo(new UsersByPincode("411038", Set.of("1234")))));
        assertThat(store.get("411057"), is(equalTo(new UsersByPincode("411057", Set.of("1234")))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("411038", new UsersByPincode("411038", Set.of("1234"))))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("411057", new UsersByPincode("411057", Set.of("1234"))))));

        // now user updates the district to 208
        when(metadataStore.fetchPincodesByDistrictId(208)).thenReturn(List.of(
                Pincode.builder()
                        .district(new District(208, "Shimla", new State(13, "Himachal Pradesh")))
                        .pincode("126112").build()));

        inputTopic.pipeInput("1234", new UserRequest("1234", List.of("411038"), List.of(208), null, null, null, null));

        // store should have user 1234 for pincode 411038 (set directly), 126112 (via district) and not 411057 (removed)
        assertThat(store.get("411038"), is(equalTo(new UsersByPincode("411038", Set.of("1234")))));
        assertThat(store.get("126112"), is(equalTo(new UsersByPincode("126112", Set.of("1234")))));
        assertThat(store.get("411057"), is(equalTo(new UsersByPincode("411057", Set.of()))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("411038", new UsersByPincode("411038", Set.of("1234"))))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("126112", new UsersByPincode("126112", Set.of("1234"))))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("411057", new UsersByPincode("411057", Set.of())))));

        // now user removes the district preference
        inputTopic.pipeInput("1234", new UserRequest("1234", List.of("411038"), List.of(), null, null, null, null));

        // store should have user 1234 for pincode 411038 (set directly) and not 126112 (removed)
        assertThat(store.get("411038"), is(equalTo(new UsersByPincode("411038", Set.of("1234")))));
        assertThat(store.get("126112"), is(equalTo(new UsersByPincode("126112", Set.of()))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("411038", new UsersByPincode("411038", Set.of("1234"))))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("126112", new UsersByPincode("126112", Set.of())))));
    }

    @Test
    public void shouldUpdateStoreWhenUserSendsMultipleDistrict() {
        // mock metadata store
        when(metadataStore.fetchPincodesByDistrictId(363)).thenReturn(List.of(
                Pincode.builder()
                        .district(new District(363, "Pune", new State(21, "Maharashtra")))
                        .pincode("411057").build()));
        when(metadataStore.fetchPincodesByDistrictId(208)).thenReturn(List.of(
                Pincode.builder()
                        .district(new District(208, "Shimla", new State(13, "Himachal Pradesh")))
                        .pincode("126112").build()));

        // User 1234 has pincode 411038 set in setup()
        // Now user 1234 sets district 363
        inputTopic.pipeInput("1234", new UserRequest("1234", List.of("411038"), List.of(363, 208), null, null, null, null));

        // store should have user 1234 for pincode 411038 (set directly), 411057 and 126112 (via district)
        assertThat(store.get("411038"), is(equalTo(new UsersByPincode("411038", Set.of("1234")))));
        assertThat(store.get("411057"), is(equalTo(new UsersByPincode("411057", Set.of("1234")))));
        assertThat(store.get("126112"), is(equalTo(new UsersByPincode("126112", Set.of("1234")))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("411038", new UsersByPincode("411038", Set.of("1234"))))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("411057", new UsersByPincode("411057", Set.of("1234"))))));
        assertThat(outputTopic.readKeyValue(), is(equalTo(new KeyValue<>("126112", new UsersByPincode("126112", Set.of("1234"))))));
    }
}
