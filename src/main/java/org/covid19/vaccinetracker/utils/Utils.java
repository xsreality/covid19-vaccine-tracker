package org.covid19.vaccinetracker.utils;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.telegram.telegrambots.meta.api.objects.Chat;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;

import static java.util.Objects.nonNull;

public class Utils {
    private static final String PINCODE_REGEX_PATTERN = "^[1-9][0-9]{5}$";
    private static final String INDIA_TIMEZONE = "Asia/Kolkata";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static boolean allValidPincodes(@NotNull String pincodes) {
        return Arrays
                .stream(pincodes.trim().split("\\s*,\\s*"))
                .allMatch(pincode -> pincode.matches(PINCODE_REGEX_PATTERN));
    }

    public static List<String> splitPincodes(@NotNull String pincodes) {
        return Arrays.asList(pincodes.trim().split("\\s*,\\s*"));
    }

    public static String translateName(Chat chat) {
        if (nonNull(chat.getFirstName())) {
            if (nonNull(chat.getLastName())) {
                return chat.getFirstName() + " " + chat.getLastName();
            }
            return chat.getFirstName();
        } else if (nonNull(chat.getUserName())) {
            return chat.getUserName();
        }
        return "";
    }

    public static ZonedDateTime dateFromString(String lastNotifiedAt) {
        return ZonedDateTime.parse(lastNotifiedAt, dtf);
    }

    public static String currentTime() {
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE));
        return dateTime.format(dtf);
    }

    public static String todayIST() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE));
        return dateTime.format(dtf);
    }

    public static boolean dayOld(String lastNotifiedAt) {
        ZonedDateTime notifiedAt = dateFromString(lastNotifiedAt);
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE));
        return Duration.between(notifiedAt, currentTime)
                .compareTo(Duration.ofHours(24)) >= 0;
    }

    public static String buildNotificationMessage(List<Center> eligibleCenters) {
        StringBuilder text = new StringBuilder();
        for (Center center : eligibleCenters) {
            text.append(String.format("%s (%s %s)\n", center.name, center.districtName, center.pincode));
            for (Session session : center.sessions) {
                text.append(String.format("%s dose(s) of %s for %s+ age group available on %s\n", session.availableCapacity, session.vaccine, session.minAgeLimit, session.date));
            }
        }
        return text.toString();
    }
}
