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
            entry("Hindi", "(%s+ आयु वर्ग के लिए %s की %s खुराकें (खुराक 1: %s, खुराक 2: %s) %s को उपलब्ध हैं)"),
            entry("Telugu", "(%s+ ఏళ్ళ వయస్సు గల %s మోతాదుల %s (మోతాదుల 1: %s, మోతాదుల 2: %s) %s న లభిస్తుంది)"),
            entry("Gujarati", "(%s+ વયના લોકો માટે %s ના %s ડોઝ (ડોઝ 1: %s, ડોઝ 2: %s) %s પર ઉપલબ્ધ છે)"),
            entry("Kannada", "(%s+ ವರ್ಷ ವಯಸ್ಸಿನವರಿಗೆ %s ಡೋಸ್ %s (ಡೋಸ್ 1: %s, ಡೋಸ್ 2: %s) %s ರಂದು ಲಭ್ಯವಿದೆ)"),
            entry("Malayalam", "(%s+ വയസ്സിനിടയിൽ, %s ന്റെ %s ഡോസുകൾ (ഡോസുകൾ 1: %s, ഡോസുകൾ 2: %s) %s ന് ലഭ്യമാണ്)"),
            entry("Marathi", "(%s+ वयोगटातील %s %s डोस (डोस 1: %s, डोस 2: %s) %s रोजी उपलब्ध आहेत)"),
            entry("Odia", "(%s+ ବୟସ ବର୍ଗ ପାଇଁ %s ର %s ଡୋଜ୍ (ଡୋଜ୍ 1: %s, ଡୋଜ୍ 2: %s) %s ରେ ଉପଲବ୍ଧ |)"),
            entry("Tamil", "(%s+ வயதிற்குட்பட்ட %s இன் %s டோஸ் (டோஸ் 1: %s, டோஸ் 2: %s) %s அன்று கிடைக்கிறது)"),
            entry("Punjabi", "(%s+ ਸਾਲ ਦੀ ਉਮਰ ਦੇ ਲਈ %s ਦੀਆਂ %s ਖੁਰਾਕਾਂ (ਖੁਰਾਕਾਂ 1: %s, ਖੁਰਾਕਾਂ 2: %s) %s 'ਤੇ ਉਪਲਬਧ ਹਨ)"),
            entry("Bengali", "(%s+ বয়সের গোষ্ঠীর জন্য %s এর %s টি ডোজ (ডোজ 1: %s, ডোজ 2: %s) %s এ উপলব্ধ)")
    );

    private static final Map<String, String> LOCALIZED_ACK_TEXT = Map.ofEntries(
            entry("Hindi", "ठीक है! जब आपके स्थान के पास के केंद्रों में टीका उपलब्ध होगा तो मैं आपको सूचित करूँगा।\n" +
                    "आप कई पिन कोड कॉमा (,) द्वारा अलग-अलग सेट कर सकते हैं। अधिकतम 3 पिन कोड की अनुमति है।\n" +
                    "सुनिश्चित करें कि अधिसूचना इस बॉट के लिए चालू है ताकि आप किसी भी अलर्ट को न भूलें!\n\n" +
                    "अपनी आयु वरीयता निर्धारित करने के लिए /age भेजें।\n\n" +
                    "अपनी वर्तमान सदस्यता देखने के लिए /subscriptions भेजें।"),
            entry("Telugu", "సరే! మీ ప్రదేశానికి సమీపంలో ఉన్న కేంద్రాల్లో టీకా అందుబాటులో ఉన్నప్పుడు నేను మీకు తెలియజేస్తాను.\n" +
                    "కామా (,) తో వేరు చేసి వాటిని పంపించడం ద్వారా మీరు బహుళ పిన్\u200Cకోడ్\u200Cలను సెట్ చేయవచ్చు. గరిష్టంగా 3 పిన్\u200Cకోడ్\u200Cలు అనుమతించబడతాయి.\n" +
                    "ఈ బోట్ కోసం నోటిఫికేషన్ ఆన్ చేయబడిందని నిర్ధారించుకోండి, అందువల్ల మీరు ఎటువంటి హెచ్చరికలను కోల్పోరు!"),
            entry("Gujarati", "બરાબર! જ્યારે તમારા સ્થાન નજીકના કેન્દ્રોમાં રસી ઉપલબ્ધ હોય ત્યારે હું તમને જાણ કરીશ.\n" +
                    "તમે બહુવિધ પિનકોડ તેમને અલ્પવિરામ (,) દ્વારા અલગ કરીને મોકલીને સેટ કરી શકો છો. વધુમાં વધુ 3 પિનકોડ માન્ય છે.\n" +
                    "ખાતરી કરો કે આ બotટ માટે સૂચના ચાલુ છે જેથી તમે કોઈપણ ચેતવણીઓ ચૂકશો નહીં!"),
            entry("Kannada", "ಸರಿ! ನಿಮ್ಮ ಸ್ಥಳದ ಸಮೀಪವಿರುವ ಕೇಂದ್ರಗಳಲ್ಲಿ ಲಸಿಕೆ ಲಭ್ಯವಿರುವಾಗ ನಾನು ನಿಮಗೆ ತಿಳಿಸುತ್ತೇನೆ.\n" +
                    "ಅಲ್ಪವಿರಾಮದಿಂದ (,) ಬೇರ್ಪಡಿಸಿ ಒಟ್ಟಿಗೆ ಕಳುಹಿಸುವ ಮೂಲಕ ನೀವು ಅನೇಕ ಪಿನ್\u200Cಕೋಡ್\u200Cಗಳನ್ನು ಹೊಂದಿಸಬಹುದು. ಗರಿಷ್ಠ 3 ಪಿನ್\u200Cಕೋಡ್\u200Cಗಳನ್ನು ಅನುಮತಿಸಲಾಗಿದೆ.\n" +
                    "ಈ ಬೋಟ್\u200Cಗಾಗಿ ಅಧಿಸೂಚನೆಯನ್ನು ಆನ್ ಮಾಡಲಾಗಿದೆ ಎಂದು ಖಚಿತಪಡಿಸಿಕೊಳ್ಳಿ ಆದ್ದರಿಂದ ನೀವು ಯಾವುದೇ ಎಚ್ಚರಿಕೆಗಳನ್ನು ಕಳೆದುಕೊಳ್ಳಬೇಡಿ!"),
            entry("Malayalam", "ശരി! നിങ്ങളുടെ സ്ഥലത്തിനടുത്തുള്ള കേന്ദ്രങ്ങളിൽ വാക്സിൻ ലഭ്യമാകുമ്പോൾ ഞാൻ നിങ്ങളെ അറിയിക്കും.\n" +
                    "കോമ (,) ഉപയോഗിച്ച് വേർതിരിച്ച് ഒന്നിച്ച് അയച്ചുകൊണ്ട് നിങ്ങൾക്ക് ഒന്നിലധികം പിൻകോഡുകൾ സജ്ജമാക്കാൻ കഴിയും. പരമാവധി 3 പിൻ\u200Cകോഡുകൾ അനുവദനീയമാണ്.\n" +
                    "ഈ ബോട്ടിനായി അറിയിപ്പ് ഓണാണെന്ന് ഉറപ്പാക്കുക, അതിനാൽ നിങ്ങൾക്ക് അലേർട്ടുകളൊന്നും നഷ്\u200Cടമാകില്ല!"),
            entry("Marathi", "ठीक आहे! जेव्हा आपल्या स्थानाजवळील केंद्रांमध्ये लस उपलब्ध असेल तेव्हा मी आपल्याला सूचित करतो.\n" +
                    "आपण एकाधिक पिन कोड स्वल्पविरामाने विभक्त करून (,) पाठवून ते सेट करू शकता. जास्तीत जास्त 3 पिनकोड अनुमत आहेत.\n" +
                    "या बॉटसाठी सूचना चालू असल्याचे सुनिश्चित करा जेणेकरून आपण कोणताही सतर्क गमावू नका!"),
            entry("Odia", "ଠିକ ଅଛି! ଯେତେବେଳେ ତୁମର ଅବସ୍ଥାନ ନିକଟ କେନ୍ଦ୍ରଗୁଡ଼ିକରେ ଟିକା ଉପଲବ୍ଧ ହେବ ମୁଁ ତୁମକୁ ଜଣାଇବି |\n" +
                    "ସେମାନଙ୍କୁ କମା (,) ଦ୍ୱାରା ପୃଥକ ଭାବରେ ପଠାଇ ଏକାଧିକ ପିନକୋଡ୍ ସେଟ୍ କରିପାରିବେ | ସର୍ବାଧିକ 3 ପିଙ୍କୋଡ୍ ଅନୁମତିପ୍ରାପ୍ତ |\n" +
                    "ନିଶ୍ଚିତ କରନ୍ତୁ ଯେ ଏହି ବଟ୍ ପାଇଁ ବିଜ୍ଞପ୍ତି ଅନ୍ ଅଛି ତେଣୁ ଆପଣ କ any ଣସି ସତର୍କତାକୁ ହାତଛଡ଼ା କରିବେ ନାହିଁ!"),
            entry("Tamil", "சரி! உங்கள் இருப்பிடத்திற்கு அருகிலுள்ள மையங்களில் தடுப்பூசி கிடைக்கும்போது நான் உங்களுக்கு அறிவிப்பேன்.\n" +
                    "கமாவால் (,) பிரிக்கப்பட்ட ஒன்றாக அனுப்புவதன் மூலம் பல பின்கோட்களை அமைக்கலாம். அதிகபட்சம் 3 பின்கோட்கள் அனுமதிக்கப்படுகின்றன.\n" +
                    "இந்த போட்டுக்கான அறிவிப்பு இயக்கப்பட்டிருப்பதை உறுதிசெய்க, எனவே நீங்கள் எந்த எச்சரிக்கைகளையும் தவறவிடாதீர்கள்!"),
            entry("Punjabi", "ਠੀਕ ਹੈ! ਜਦੋਂ ਮੈਂ ਤੁਹਾਡੇ ਸਥਾਨ ਦੇ ਨੇੜੇ ਕੇਂਦਰਾਂ ਵਿੱਚ ਟੀਕਾ ਉਪਲਬਧ ਕਰਵਾਵਾਂਗਾ ਤਾਂ ਮੈਂ ਤੁਹਾਨੂੰ ਸੂਚਿਤ ਕਰਾਂਗਾ.\n" +
                    "ਤੁਸੀਂ ਕਈ ਪਿੰਨਕੋਡ ਸੈੱਟ ਕਰ ਸਕਦੇ ਹੋ ਉਹਨਾਂ ਨੂੰ ਕਾਮੇ (,) ਦੁਆਰਾ ਵੱਖ ਕਰਕੇ ਭੇਜ ਕੇ. ਵੱਧ ਤੋਂ ਵੱਧ 3 ਪਿੰਨਕੋਡ ਦੀ ਆਗਿਆ ਹੈ.\n" +
                    "ਇਹ ਸੁਨਿਸ਼ਚਿਤ ਕਰੋ ਕਿ ਇਸ ਬੋਟ ਲਈ ਨੋਟੀਫਿਕੇਸ਼ਨ ਚਾਲੂ ਹੈ ਤਾਂ ਕਿ ਤੁਸੀਂ ਕੋਈ ਵੀ ਚੇਤਾਵਨੀ ਨਾ ਗੁਆਓ!"),
            entry("Bengali", "ঠিক আছে! আপনার অবস্থানের নিকটবর্তী কেন্দ্রে যখন ভ্যাকসিন পাওয়া যায় তখন আমি আপনাকে অবহিত করব।\n" +
                    "আপনি একাধিক পিনকোডগুলি কমা দ্বারা পৃথক করে প্রেরণ করে সেট করতে পারেন (,)। সর্বাধিক 3 পিনকোড অনুমোদিত।\n" +
                    "নিশ্চিত হয়ে নিন যে এই বটের জন্য বিজ্ঞপ্তি চালু আছে যাতে আপনি কোনও সতর্কতা মিস করেন না!")
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
                        session.vaccine, session.minAgeLimit, humanReadable(session.date), ofNullable(session.getCost()).map(s -> "₹" + s).orElse("Unknown")));
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
