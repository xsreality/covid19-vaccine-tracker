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
import org.springframework.scheduling.annotation.Scheduled;
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
    private final NotificationStats stats;
    private final NotificationCache cache;

    public KafkaNotifications(StreamsBuilder streamsBuilder, KTable<String, UsersByPincode> usersByPincodeTable,
                              VaccinePersistence vaccinePersistence, VaccineCentersProcessor vaccineCentersProcessor,
                              BotService botService, NotificationStats stats, NotificationCache cache) {
        this.streamsBuilder = streamsBuilder;
        this.usersByPincodeTable = usersByPincodeTable;
        this.vaccinePersistence = vaccinePersistence;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.botService = botService;
        this.stats = stats;
        this.cache = cache;
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
            stats.incrementProcessedPincodes();
            users.forEach(user -> Stream.ofNullable(vaccineCenters)
                    .peek(vc -> stats.incrementUserRequests())
                    .filter(centersWithData())
                    .map(eligibleCentersFor(user))
                    .peek(logEmptyCenters(pincode))
                    .filter(eligibleCentersWithData())
                    .forEach(eligibleCenters -> {
                        if (cache.isNewNotification(user, eligibleCenters)) {
                            log.debug("Slots data changed since {} was last notified", user);
                            if (botService.notify(user, eligibleCenters)) {
                                stats.incrementNotificationsSent();
                            } else {
                                stats.incrementNotificationsErrors();
                            }
                            cache.updateUser(user, eligibleCenters);
                        } else {
                            log.debug("No difference in slots data since {} was last notified", user);
                        }
                        vaccinePersistence.markProcessed(vaccineCenters); // mark processed
                    }));
        });

        return stream;
    }

    /*
     * Crude way to measure notification stats in async scenario
     */
    @Scheduled(cron = "${jobs.cron.notification.stats:-}", zone = "IST")
    public void logAndResetStats() {
        log.info("[NOTIFICATION] Users: {}, Pincodes: {}, Sent: {}, Errors: {}",
                stats.userRequests(), stats.processedPincodes(),
                stats.notificationsSent(), stats.notificationsErrors());
        botService.notifyOwner(String.format("[NOTIFICATION] Users: %d, Pincodes: %d, Sent: %d, Errors: %d",
                stats.userRequests(), stats.processedPincodes(),
                stats.notificationsSent(), stats.notificationsErrors()));
        stats.reset();
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
