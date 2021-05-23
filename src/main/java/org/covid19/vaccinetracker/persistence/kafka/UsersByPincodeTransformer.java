package org.covid19.vaccinetracker.persistence.kafka;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.UsersByPincode;

import java.util.HashSet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UsersByPincodeTransformer implements Transformer<String, UserRequest, KeyValue<String, UsersByPincode>> {
    private ProcessorContext ctx;
    private KeyValueStore<String, UsersByPincode> aggregateStore;
    private final String AGGREGATE_STORE_NAME;

    public UsersByPincodeTransformer(String aggregateStoreName) {
        this.AGGREGATE_STORE_NAME = aggregateStoreName;
    }

    @Override
    public void init(ProcessorContext context) {
        log.info("inside init() method)");
        this.ctx = context;
        //noinspection unchecked
        this.aggregateStore = (KeyValueStore<String, UsersByPincode>) context.getStateStore(this.AGGREGATE_STORE_NAME);
    }

    @Override
    public KeyValue<String, UsersByPincode> transform(String userId, UserRequest userRequest) {
        KeyValue<String, UsersByPincode> toForward = null;

        // TODO: handle empty pincode (/stop), should cleanup old requests of this user

        /*
         * TODO: handle cleanup of old requests when new one arrives for same user
         * e.g. given 2 events
         * user A -> 177001
         * user A -> 177401
         * state store should update value of key 177001 and remove user A
         * maybe use UserRequest as input for this transformer?
         */

        userRequest.getPincodes().forEach(pincode -> {
            maybeInitializeNewEventInStateStore(pincode);

            UsersByPincode aggregatedUsersByPincode = aggregateStore.get(pincode).merge(userId);
            rememberNewEvent(pincode, aggregatedUsersByPincode);

            log.debug("aggregate: {}", aggregatedUsersByPincode);
            ctx.forward(pincode, aggregatedUsersByPincode);
        });


        return toForward;
    }

    private void maybeInitializeNewEventInStateStore(final String eventId) {
        aggregateStore.putIfAbsent(eventId, new UsersByPincode(eventId, new HashSet<>()));
    }

    private void rememberNewEvent(final String eventId, UsersByPincode aggregated) {
        aggregateStore.put(eventId, aggregated);
    }

    @Override
    public void close() {

    }
}
