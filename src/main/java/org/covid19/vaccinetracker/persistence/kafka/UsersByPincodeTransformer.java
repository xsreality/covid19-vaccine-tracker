package org.covid19.vaccinetracker.persistence.kafka;

import com.google.common.annotations.VisibleForTesting;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.covid19.vaccinetracker.model.UsersByPincode;
import org.covid19.vaccinetracker.userrequests.MetadataStore;
import org.covid19.vaccinetracker.userrequests.model.Pincode;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UsersByPincodeTransformer implements Transformer<String, UserRequest, KeyValue<String, UsersByPincode>> {
    private ProcessorContext ctx;
    private KeyValueStore<String, UsersByPincode> aggregateStore;
    private final String AGGREGATE_STORE_NAME;
    private MetadataStore metadataStore;

    public UsersByPincodeTransformer(String aggregateStoreName, MetadataStore metadataStore) {
        this.metadataStore = metadataStore;
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

        if (userRequest.getPincodes().isEmpty()) {
            log.debug("Removing all references to {} in state store", userId);
            cleanupUserInStateStore(userId, List.of());
            return null; // nothing else to forward
        }

        /*
         * Pincodes come from two sources:
         * - Set directly by the user
         * - From the district set by the user
         * We combine the two sources of pincodes before applying the transforming function
         */
        List<String> combinedPincodes =
                Stream.concat(
                        userRequest.getPincodes().stream(),
                        streamPincodesFromDistrict(userRequest.getDistricts())
                ).collect(Collectors.toList());

        combinedPincodes.stream().distinct().forEach(pincode -> {
            log.debug("current data in state store: {}", aggregateStore.get(pincode));

            if (pincode.isBlank()) {
                return; // very unlikely to happen but ¯\_(ツ)_/¯
            }

            maybeInitializeNewEventInStateStore(pincode);

            UsersByPincode aggregatedUsersByPincode = aggregateStore.get(pincode).merge(userId);
            rememberNewEvent(pincode, aggregatedUsersByPincode);

            log.debug("aggregate: {}", aggregatedUsersByPincode);
            ctx.forward(pincode, aggregatedUsersByPincode);
        });

        cleanupUserInStateStore(userId, combinedPincodes);

        return null;
    }

    private Stream<String> streamPincodesFromDistrict(List<Integer> districts) {
        return Optional.ofNullable(districts)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(this::fetchPincodesFromMetadataStore)
                ;
    }

    private Stream<String> fetchPincodesFromMetadataStore(Integer districtId) {
        return metadataStore.fetchPincodesByDistrictId(districtId)
                .stream().map(Pincode::getPincode);
    }

    /*
     * Iterates through all pincodes in state store and
     * removes any references of given user. exceptionPincodes
     * are not modified.
     */
    private void cleanupUserInStateStore(String userId, List<String> exceptionPincodes) {
        final KeyValueIterator<String, UsersByPincode> it = aggregateStore.all();
        log.debug("Starting cleanup");
        while (it.hasNext()) {
            final KeyValue<String, UsersByPincode> entry = it.next();
            String pincode = entry.key;
            log.debug("pincode: {}", pincode);

            if (exceptionPincodes.contains(pincode)) { // skip excepted pincodes
                log.debug("pincode in exception list: {}", pincode);
                continue;
            }

            final Set<String> users = entry.value.getUsers();

            if (users.remove(userId)) {
                log.debug("Removing subscribed user {} for pincode {}", userId, pincode);
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
