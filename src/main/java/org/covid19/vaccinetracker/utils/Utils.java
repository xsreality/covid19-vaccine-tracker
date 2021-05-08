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
import java.util.Map;

import javax.validation.constraints.NotNull;

import static java.util.Map.entry;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class Utils {
    private static final String PINCODE_REGEX_PATTERN = "^[1-9][0-9]{5}$";
    private static final String INDIA_TIMEZONE = "Asia/Kolkata";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final Map<String, String> STATE_LANGUAGES = Map.ofEntries(
            entry("Andhra Pradesh", "Telugu"),
            entry("Gujarat", "Gujarati"),
            entry("Karnataka", "Kannada"),
            entry("Kerala", "Malayalam"),
            entry("Lakshadweep", "Malayalam"),
            entry("Maharashtra", "Marathi"),
            entry("Odisha", "Odia"),
            entry("Puducherry", "Tamil"),
            entry("Punjab", "Punjabi"),
            entry("Tamil Nadu", "Tamil"),
            entry("Telangana", "Telugu"),
            entry("Tripura", "Bengali"),
            entry("West Bengal", "Bengali")
    );

    private static final Map<String, String> LOCALIZED_NOTIFICATION_TEXT = Map.ofEntries(
            entry("Hindi", "(%s+ आयु वर्ग के लिए %s की %s खुराकें %s को उपलब्ध हैं)"),
            entry("Telugu", "(%s+ ఏళ్ళ వయస్సు గల %s మోతాదుల %s %s న లభిస్తుంది)"),
            entry("Gujarati", "(%s+ વયના લોકો માટે %s ના %s ડોઝ %s પર ઉપલબ્ધ છે)"),
            entry("Kannada", "(%s+ ವರ್ಷ ವಯಸ್ಸಿನವರಿಗೆ %s ಡೋಸ್ %s %s ರಂದು ಲಭ್ಯವಿದೆ)"),
            entry("Malayalam", "(%s+ വയസ്സിനിടയിൽ, %s ന്റെ %s ഡോസുകൾ %s ന് ലഭ്യമാണ്)"),
            entry("Marathi", "(%s+ वयोगटातील %s %s डोस %s रोजी उपलब्ध आहेत)"),
            entry("Odia", "(%s+ ବୟସ ବର୍ଗ ପାଇଁ %s ର %s ଡୋଜ୍ %s ରେ ଉପଲବ୍ଧ |)"),
            entry("Tamil", "(%s+ வயதிற்குட்பட்ட %s இன் %s டோஸ் %s அன்று கிடைக்கிறது)"),
            entry("Punjabi", "(%s+ ਸਾਲ ਦੀ ਉਮਰ ਦੇ ਲਈ %s ਦੀਆਂ %s ਖੁਰਾਕਾਂ %s 'ਤੇ ਉਪਲਬਧ ਹਨ)"),
            entry("Bengali", "(%s+ বয়সের গোষ্ঠীর জন্য %s এর %s টি ডোজ %s এ উপলব্ধ)")
    );

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

    public static boolean pastHalfHour(String lastNotifiedAt) {
        ZonedDateTime notifiedAt = dateFromString(lastNotifiedAt);
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE));
        return Duration.between(notifiedAt, currentTime)
                .compareTo(Duration.ofMinutes(30L)) >= 0;
    }

    public static String buildNotificationMessage(List<Center> eligibleCenters) {
        StringBuilder text = new StringBuilder();
        for (Center center : eligibleCenters) {
            text.append(String.format("%s (%s %s)\n", center.name, center.districtName, center.pincode));
            for (Session session : center.sessions) {
                text.append(String.format("%s dose(s) of %s for %s+ age group available on %s ", session.availableCapacity, session.vaccine, session.minAgeLimit, session.date));
                text.append(String.format(localizedText(center.getStateName()) + "\n", session.minAgeLimit, session.vaccine, session.availableCapacity, session.date));
            }
            text.append("\n");
        }
        text.append("For registration, please visit https://selfregistration.cowin.gov.in/\n");
        return text.toString();
    }

    public static String localizedText(String stateName) {
        if (isNull(stateName)) {
            return LOCALIZED_NOTIFICATION_TEXT.get("Hindi");
        }
        String language = STATE_LANGUAGES.getOrDefault(stateName, "Hindi");
        return LOCALIZED_NOTIFICATION_TEXT.get(language);
    }
}
