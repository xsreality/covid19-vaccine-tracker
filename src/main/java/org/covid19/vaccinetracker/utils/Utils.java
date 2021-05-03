package org.covid19.vaccinetracker.utils;

import org.telegram.telegrambots.meta.api.objects.Chat;

import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;

import static java.util.Objects.nonNull;

public class Utils {
    private static final String PINCODE_REGEX_PATTERN = "^[1-9][0-9]{5}$";

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
}
