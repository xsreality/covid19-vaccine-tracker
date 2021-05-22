package org.covid19.vaccinetracker.persistence.kafka;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.covid19.vaccinetracker.model.UsersByPincode;

import java.util.HashSet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UsersByPincodeProcessor implements Transformer<String, String, KeyValue<String, UsersByPincode>> {
    private KeyValueStore<String, UsersByPincode> aggregateStore;
    private final String AGGREGATE_STORE_NAME;

    public UsersByPincodeProcessor(String aggregateStoreName) {
        this.AGGREGATE_STORE_NAME = aggregateStoreName;
    }

    @Override
    public void init(ProcessorContext context) {
        log.info("inside init() method)");
        //noinspection unchecked
        this.aggregateStore = (KeyValueStore<String, UsersByPincode>) context.getStateStore(this.AGGREGATE_STORE_NAME);
    }

    @Override
    public KeyValue<String, UsersByPincode> transform(String userId, String pincode) {
        KeyValue<String, UsersByPincode> toForward;

        // TODO: handle empty pincode

        initializeNewEventInStateStore(pincode);

        UsersByPincode aggregatedUsersByPincode = aggregateStore.get(pincode).merge(userId);
        rememberNewEvent(pincode, aggregatedUsersByPincode);

        log.debug("aggregate: {}", aggregatedUsersByPincode);
        toForward = KeyValue.pair(pincode, aggregatedUsersByPincode);
        return toForward;
    }

    private void initializeNewEventInStateStore(final String eventId) {
        aggregateStore.putIfAbsent(eventId, new UsersByPincode(eventId, new HashSet<>()));
    }

    private void rememberNewEvent(final String eventId, UsersByPincode aggregated) {
        aggregateStore.put(eventId, aggregated);
    }

    @Override
    public void close() {

    }
}
