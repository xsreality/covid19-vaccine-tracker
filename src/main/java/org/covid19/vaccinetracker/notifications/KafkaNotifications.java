package org.covid19.vaccinetracker.notifications;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.covid19.vaccinetracker.availability.VaccineCentersProcessor;
import org.covid19.vaccinetracker.bot.BotService;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.UsersByPincode;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaNotifications {
    @Value("${topic.updated.pincodes}")
    private String updatedPincodesTopic;

    @Value("${users.over45}")
    private List<String> usersOver45;

    private final StreamsBuilder streamsBuilder;
    private final KTable<String, UsersByPincode> usersByPincodeTable;
    private final VaccinePersistence vaccinePersistence;
    private final VaccineCentersProcessor vaccineCentersProcessor;
    private final BotService botService;

    public KafkaNotifications(StreamsBuilder streamsBuilder, KTable<String, UsersByPincode> usersByPincodeTable,
                              VaccinePersistence vaccinePersistence, VaccineCentersProcessor vaccineCentersProcessor, BotService botService) {
        this.streamsBuilder = streamsBuilder;
        this.usersByPincodeTable = usersByPincodeTable;
        this.vaccinePersistence = vaccinePersistence;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.botService = botService;
    }

    @Bean
    KStream<String, Set<String>> notificationsStream() {
        log.debug("Building notifications KStreams");
        final KStream<String, Set<String>> stream =
                streamsBuilder.stream(updatedPincodesTopic, Consumed.with(Serdes.String(), Serdes.String()))
                        .join(usersByPincodeTable, (pincode, usersByPincode) -> usersByPincode)
                        .mapValues((pincode, value) -> value.getUsers());

        // send notifications
        stream.foreach((pincode, users) -> {
            log.debug("Building notifications for pincode {} and users {}", pincode, users);
            final VaccineCenters vaccineCenters = vaccinePersistence.fetchVaccineCentersByPincode(pincode);
            users.forEach(user -> Stream.ofNullable(vaccineCenters)
                    .filter(centersWithData())
                    .map(eligibleCentersFor(user))
                    .peek(logEmptyCenters(pincode))
                    .filter(eligibleCentersWithData())
                    .forEach(eligibleCenters -> {
                        botService.notify(user, eligibleCenters);
                        vaccinePersistence.markProcessed(vaccineCenters); // mark processed
                    }));
        });

        return stream;
    }

    @NotNull
    private Predicate<List<Center>> eligibleCentersWithData() {
        return centers -> !centers.isEmpty();
    }

    @NotNull
    private Consumer<List<Center>> logEmptyCenters(String pincode) {
        return centers -> {
            if (centers.isEmpty()) {
                log.debug("No eligible vaccine centers found for pin code {}", pincode);
            }
        };
    }

    @NotNull
    private Function<VaccineCenters, List<Center>> eligibleCentersFor(String user) {
        return vc -> vaccineCentersProcessor.eligibleVaccineCenters(vc, usersOver45.contains(user));
    }

    @NotNull
    private Predicate<VaccineCenters> centersWithData() {
        return vc -> !vc.getCenters().isEmpty();
    }
}
