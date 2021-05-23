package org.covid19.vaccinetracker.persistence.kafka;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.UsersByPincode;

import java.util.HashSet;
import java.util.Set;

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
        log.debug("UsersByPincodeTransformer init() called)");
        this.ctx = context;
        //noinspection unchecked
        this.aggregateStore = (KeyValueStore<String, UsersByPincode>) context.getStateStore(this.AGGREGATE_STORE_NAME);
    }

    @Override
    public KeyValue<String, UsersByPincode> transform(String userId, UserRequest userRequest) {
        log.debug("Entering transform for {}", userRequest);
        KeyValue<String, UsersByPincode> toForward = null;

        if (userRequest.getPincodes().isEmpty()) {
            log.debug("Removing all references to {} in state store", userId);
            cleanupUserInStateStore(userId, Set.of());
            return null; // nothing else to forward
        }

        /*
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

            cleanupUserInStateStore(userId, Set.of(pincode));
        });

        return toForward;
    }

    /*
     * Iterates through all pincodes in state store and
     * removes any references of given user. exceptionPincodes
     * are not modified.
     */
    private void cleanupUserInStateStore(String userId, Set<String> exceptionPincodes) {
        final KeyValueIterator<String, UsersByPincode> it = aggregateStore.all();
        while (it.hasNext()) {
            final KeyValue<String, UsersByPincode> entry = it.next();
            String pincode = entry.key;

            if (exceptionPincodes.contains(pincode)) { // skip excepted pincodes
                continue;
            }

            final Set<String> users = entry.value.getUsers();

            if (users.remove(userId)) {
                final UsersByPincode updated = new UsersByPincode(pincode, users);
                aggregateStore.put(pincode, updated);
                ctx.forward(pincode, updated);
            }
        }
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
