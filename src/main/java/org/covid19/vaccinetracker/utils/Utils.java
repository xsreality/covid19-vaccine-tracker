package org.covid19.vaccinetracker.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import org.apache.commons.lang3.StringUtils;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.userrequests.model.State;
import org.springframework.web.util.HtmlUtils;
import org.telegram.telegrambots.meta.api.objects.Chat;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;

import static java.util.Map.entry;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

@Slf4j
public class Utils {
    private static final String PINCODE_REGEX_PATTERN = "^[1-9][0-9]{5}$";
    public static final String INDIA_TIMEZONE = "Asia/Kolkata";
    public static final DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final Map<String, String> STATE_LANGUAGES = Map.ofEntries(
            entry("Andaman and Nicobar Islands", "Bengali"),
            entry("Andhra Pradesh", "Telugu"),
            entry("Dadra and Nagar Haveli", "Gujarati"),
            entry("Daman and Diu", "Gujarati"),
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
            entry("Hindi", "(%s+ ????????? ???????????? ?????? ????????? %s ?????? %s ????????????????????? (??????????????? 1: %s, ??????????????? 2: %s) %s ?????? ?????????????????? ?????????)"),
            entry("Telugu", "(%s+ ???????????? ?????????????????? ?????? %s ????????????????????? %s (????????????????????? 1: %s, ????????????????????? 2: %s) %s ??? ??????????????????????????????)"),
            entry("Gujarati", "(%s+ ???????????? ???????????? ???????????? %s ?????? %s ????????? (????????? 1: %s, ????????? 2: %s) %s ?????? ?????????????????? ??????)"),
            entry("Kannada", "(%s+ ???????????? ???????????????????????????????????? %s ???????????? %s (???????????? 1: %s, ???????????? 2: %s) %s ???????????? ????????????????????????)"),
            entry("Malayalam", "(%s+ ????????????????????????????????????, %s ???????????? %s ?????????????????? (?????????????????? 1: %s, ?????????????????? 2: %s) %s ?????? ????????????????????????)"),
            entry("Marathi", "(%s+ ??????????????????????????? %s %s ????????? (????????? 1: %s, ????????? 2: %s) %s ???????????? ?????????????????? ????????????)"),
            entry("Odia", "(%s+ ????????? ???????????? ???????????? %s ??? %s ???????????? (???????????? 1: %s, ???????????? 2: %s) %s ?????? ?????????????????? |)"),
            entry("Tamil", "(%s+ ?????????????????????????????????????????? %s ????????? %s ???????????? (???????????? 1: %s, ???????????? 2: %s) %s ??????????????? ?????????????????????????????????)"),
            entry("Punjabi", "(%s+ ????????? ?????? ????????? ?????? ?????? %s ???????????? %s ????????????????????? (????????????????????? 1: %s, ????????????????????? 2: %s) %s '?????? ??????????????? ??????)"),
            entry("Bengali", "(%s+ ?????????????????? ????????????????????? ???????????? %s ?????? %s ?????? ????????? (????????? 1: %s, ????????? 2: %s) %s ??? ??????????????????)")
    );

    private static final Map<String, String> LOCALIZED_ACK_TEXT = Map.ofEntries(
            entry("Hindi", "????????? ??????! ?????? ???????????? ??????????????? ?????? ????????? ?????? ???????????????????????? ????????? ???????????? ?????????????????? ???????????? ?????? ????????? ???????????? ??????????????? ?????????????????????\n" +
                    "?????? ?????? ????????? ????????? ???????????? (,) ?????????????????? ?????????-????????? ????????? ?????? ???????????? ???????????? ?????????????????? 3 ????????? ????????? ?????? ?????????????????? ?????????\n" +
                    "??????????????????????????? ???????????? ?????? ???????????????????????? ?????? ????????? ?????? ????????? ???????????? ?????? ???????????? ?????? ???????????? ?????? ??????????????? ?????? ??? ???????????????!\n\n" +
                    "???????????? ????????? ?????????????????? ??????????????????????????? ???????????? ?????? ????????? /age ??????????????????\n\n" +
                    "???????????? ????????????????????? ????????????????????? ??????????????? ?????? ????????? /subscriptions ??????????????????"),
            entry("Telugu", "?????????! ?????? ????????????????????????????????? ????????????????????? ???????????? ????????????????????????????????? ???????????? ?????????????????????????????? ?????????????????????????????? ???????????? ???????????? ???????????????????????????????????????.\n" +
                    "???????????? (,) ?????? ???????????? ???????????? ?????????????????? ???????????????????????? ?????????????????? ???????????? ???????????? ????????????\u200C????????????\u200C????????? ???????????? ????????????????????????. ??????????????????????????? 3 ????????????\u200C????????????\u200C?????? ??????????????????????????????????????????.\n" +
                    "??? ???????????? ???????????? ????????????????????????????????? ????????? ?????????????????????????????? ????????????????????????????????????????????????, ???????????????????????? ???????????? ????????????????????? ????????????????????????????????? ????????????????????????!"),
            entry("Gujarati", "???????????????! ?????????????????? ??????????????? ??????????????? ?????????????????? ????????????????????????????????? ????????? ?????????????????? ????????? ?????????????????? ????????? ???????????? ????????? ????????????.\n" +
                    "????????? ?????????????????? ?????????????????? ??????????????? ??????????????????????????? (,) ?????????????????? ????????? ??????????????? ????????????????????? ????????? ????????? ????????? ??????. ?????????????????? ????????? 3 ?????????????????? ??????????????? ??????.\n" +
                    "??????????????? ????????? ?????? ??? ???ot??? ???????????? ??????????????? ???????????? ?????? ???????????? ????????? ??????????????? ????????????????????? ??????????????? ????????????!"),
            entry("Kannada", "?????????! ??????????????? ??????????????? ??????????????????????????? ???????????????????????????????????? ??????????????? ????????????????????????????????? ???????????? ??????????????? ????????????????????????????????????.\n" +
                    "??????????????????????????????????????? (,) ??????????????????????????? ????????????????????? ???????????????????????? ???????????? ???????????? ???????????? ????????????\u200C????????????\u200C?????????????????? ?????????????????????????????????. ?????????????????? 3 ????????????\u200C????????????\u200C?????????????????? ???????????????????????????????????????.\n" +
                    "??? ????????????\u200C???????????? ??????????????????????????????????????? ????????? ??????????????????????????? ???????????? ????????????????????????????????????????????? ???????????????????????? ???????????? ?????????????????? ?????????????????????????????????????????? ??????????????????????????????????????????!"),
            entry("Malayalam", "?????????! ??????????????????????????? ?????????????????????????????????????????????????????? ??????????????????????????????????????? ????????????????????? ??????????????????????????????????????? ????????? ????????????????????? ??????????????????????????????.\n" +
                    "????????? (,) ?????????????????????????????? ????????????????????????????????? ??????????????????????????? ???????????????????????????????????? ?????????????????????????????? ?????????????????????????????? ??????????????????????????? ????????????????????????????????? ??????????????????. ????????????????????? 3 ?????????\u200C?????????????????? ????????????????????????????????????.\n" +
                    "??? ?????????????????????????????? ??????????????????????????? ??????????????????????????? ?????????????????????????????????, ?????????????????? ?????????????????????????????? ???????????????????????????????????????????????? ?????????\u200C????????????????????????!"),
            entry("Marathi", "????????? ?????????! ?????????????????? ?????????????????? ????????????????????????????????? ??????????????????????????????????????? ?????? ?????????????????? ???????????? ?????????????????? ?????? ???????????????????????? ??????????????? ????????????.\n" +
                    "????????? ?????????????????? ????????? ????????? ?????????????????????????????????????????? ?????????????????? ???????????? (,) ?????????????????? ?????? ????????? ????????? ????????????. ????????????????????? ??????????????? 3 ?????????????????? ??????????????? ????????????.\n" +
                    "?????? ????????????????????? ??????????????? ???????????? ???????????????????????? ??????????????????????????? ????????? ???????????????????????? ????????? ????????????????????? ??????????????? ??????????????? ?????????!"),
            entry("Odia", "????????? ?????????! ???????????????????????? ???????????? ????????????????????? ???????????? ????????????????????????????????????????????? ???????????? ?????????????????? ????????? ????????? ??????????????? ?????????????????? |\n" +
                    "??????????????????????????? ????????? (,) ?????????????????? ???????????? ??????????????? ???????????? ?????????????????? ????????????????????? ???????????? ??????????????????????????? | ???????????????????????? 3 ???????????????????????? ??????????????????????????????????????? |\n" +
                    "????????????????????? ?????????????????? ?????? ????????? ????????? ???????????? ??????????????????????????? ????????? ????????? ???????????? ????????? ??? any ????????? ??????????????????????????? ????????????????????? ??????????????? ???????????????!"),
            entry("Tamil", "?????????! ?????????????????? ???????????????????????????????????????????????? ?????????????????????????????? ?????????????????????????????? ??????????????????????????? ?????????????????????????????????????????? ???????????? ?????????????????????????????? ?????????????????????????????????.\n" +
                    "????????????????????? (,) ??????????????????????????????????????? ?????????????????? ????????????????????????????????? ??????????????? ?????? ????????????????????????????????? ??????????????????????????????. ?????????????????????????????? 3 ????????????????????????????????? ????????????????????????????????????????????????????????????.\n" +
                    "???????????? ????????????????????????????????? ??????????????????????????? ????????????????????????????????????????????????????????? ??????????????????????????????, ???????????? ????????????????????? ???????????? ??????????????????????????????????????????????????? ??????????????????????????????????????????!"),
            entry("Punjabi", "????????? ??????! ???????????? ????????? ?????????????????? ???????????? ?????? ???????????? ????????????????????? ???????????? ???????????? ??????????????? ??????????????????????????? ????????? ????????? ????????????????????? ??????????????? ??????????????????.\n" +
                    "??????????????? ?????? ????????????????????? ???????????? ?????? ???????????? ?????? ??????????????? ????????? ???????????? (,) ??????????????? ????????? ???????????? ????????? ??????. ????????? ????????? ????????? 3 ????????????????????? ?????? ???????????? ??????.\n" +
                    "?????? ??????????????????????????? ????????? ?????? ?????? ????????? ?????? ????????????????????????????????? ???????????? ?????? ????????? ?????? ??????????????? ????????? ?????? ????????????????????? ?????? ????????????!"),
            entry("Bengali", "????????? ?????????! ??????????????? ??????????????????????????? ??????????????????????????? ???????????????????????? ????????? ???????????????????????? ?????????????????? ???????????? ????????? ????????? ?????????????????? ??????????????? ????????????\n" +
                    "???????????? ?????????????????? ?????????????????????????????? ????????? ?????????????????? ???????????? ????????? ?????????????????? ????????? ????????? ???????????? ??????????????? (,)??? ???????????????????????? 3 ?????????????????? ???????????????????????????\n" +
                    "????????????????????? ???????????? ????????? ?????? ?????? ???????????? ???????????? ??????????????????????????? ???????????? ????????? ???????????? ???????????? ???????????? ????????????????????? ????????? ???????????? ??????!")
    );

    public static boolean allValidPincodes(@NotNull String pincodes) {
        return Arrays
                .stream(pincodes.trim().split("\\s*,\\s*"))
                .allMatch(pincode -> pincode.matches(PINCODE_REGEX_PATTERN));
    }

    public static List<String> splitPincodes(@NotNull String pincodes) {
        return Arrays.asList(pincodes.trim().split("\\s*,\\s*"));
    }

    public static List<Integer> splitDistricts(@NotNull String districts) {
        return Arrays.stream(districts.trim().split("\\s*,\\s*")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
    }

    public static String joinPincodes(List<String> pincodes) {
        return StringUtils.join(pincodes, ',');
    }

    public static String translateName(Chat chat) {
        String name = "";
        if (nonNull(chat.getFirstName())) {
            if (nonNull(chat.getLastName())) {
                name = chat.getFirstName() + " " + chat.getLastName();
            }
            name = chat.getFirstName();
        } else if (nonNull(chat.getUserName())) {
            name = chat.getUserName();
        }
        return HtmlUtils.htmlEscape(name);
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

    public static String yesterdayIST() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE)).minusDays(1L);
        return dateTime.format(dtf);
    }

    public static String tomorrowIST() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE)).plusDays(1L);
        return dateTime.format(dtf);
    }

    public static String humanReadable(String ddMMyyyy) {
        DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        Map<Long, String> ordinalNumbers = new HashMap<>(42);
        ordinalNumbers.put(1L, "1st");
        ordinalNumbers.put(2L, "2nd");
        ordinalNumbers.put(3L, "3rd");
        ordinalNumbers.put(21L, "21st");
        ordinalNumbers.put(22L, "22nd");
        ordinalNumbers.put(23L, "23rd");
        ordinalNumbers.put(31L, "31st");
        for (long d = 1; d <= 31; d++) {
            ordinalNumbers.putIfAbsent(d, "" + d + "th");
        }

        DateTimeFormatter dayOfMonthFormatter = new DateTimeFormatterBuilder()
                .appendText(ChronoField.DAY_OF_MONTH, ordinalNumbers)
                .appendPattern(" MMM")
                .toFormatter();

        LocalDate input = LocalDate.parse(ddMMyyyy, inputFormat);
        return input.format(dayOfMonthFormatter);
    }

    public static boolean dayOld(String lastNotifiedAt) {
        ZonedDateTime notifiedAt = dateFromString(lastNotifiedAt);
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE));
        return Duration.between(notifiedAt, currentTime)
                .compareTo(Duration.ofHours(24)) >= 0;
    }

    public static boolean past15mins(String lastNotifiedAt) {
        ZonedDateTime notifiedAt = dateFromString(lastNotifiedAt);
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE));
        return Duration.between(notifiedAt, currentTime)
                .compareTo(Duration.ofMinutes(15L)) >= 0;
    }

    public static String buildNotificationMessage(List<Center> eligibleCenters) {
        StringBuilder text = new StringBuilder();
        for (Center center : eligibleCenters) {
            text.append(String.format("<b>%s (%s %s) - %s</b>\n<pre>", center.name, center.districtName, center.pincode, ofNullable(center.feeType).orElse("Unknown")));
            for (Session session : center.sessions) {
                text.append(String.format("\n%s doses (Dose 1: %s, Dose 2: %s) of %s for %s+ age group available on %s for %s\n",
                        session.availableCapacity, session.availableCapacityDose1, session.availableCapacityDose2,
                        session.vaccine, session.minAgeLimit, humanReadable(session.date), ofNullable(session.getCost()).map(s -> "???" + s).orElse("Unknown")));
                text.append(String.format(localizedNotificationText(center.getStateName()) + "\n", session.minAgeLimit, session.vaccine, session.availableCapacity, session.availableCapacityDose1, session.availableCapacityDose2, humanReadable(session.date)));
            }
            text.append("</pre>\n");
        }
        // telegram messages cannot be larger than 4KB
        if (text.toString().length() > 4096) {
            return text.substring(0, 4090) + "</pre>";
        } else {
            text.append("For registration, please visit <a href=\"https://selfregistration.cowin.gov.in/\">CoWIN Website</a>\n");
            return text.toString();
        }
    }

    public static String localizedNotificationText(String stateName) {
        if (isNull(stateName)) {
            return LOCALIZED_NOTIFICATION_TEXT.get("Hindi");
        }
        String language = STATE_LANGUAGES.getOrDefault(stateName, "Hindi");
        return LOCALIZED_NOTIFICATION_TEXT.get(language);
    }

    public static String localizedAckText(State state) {
        if (isNull(state)) {
            return LOCALIZED_ACK_TEXT.get("Hindi");
        }
        String language = STATE_LANGUAGES.getOrDefault(state.getStateName(), "Hindi");
        return LOCALIZED_ACK_TEXT.get(language);
    }

    public static boolean isValidJwtToken(String token) {
        if (isNull(token)) {
            return false;
        }
        try {
            final JWT jwt = JWTParser.parse(token);
            final JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
            return jwtClaimsSet.getIssueTime().toInstant().isBefore(Instant.now())
                    && jwtClaimsSet.getExpirationTime().toInstant().isAfter(Instant.now());
        } catch (ParseException e) {
            log.debug("Error parsing JWT token: {}", e.getMessage());
            return false;
        }
    }

    public static <T> T parseLambdaResponseJson(ObjectMapper objectMapper, String responseJson, Class<T> clazz) {
        try {
            return objectMapper.readValue(responseJson, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error parsing response from Lambda: {}", e.getMessage());
            return null;
        }
    }

    public static ZonedDateTime convertToIST(LocalDateTime time) {
        return time.atZone(ZoneId.of(INDIA_TIMEZONE));
    }
}
