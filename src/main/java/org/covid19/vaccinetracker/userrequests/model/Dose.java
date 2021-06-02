package org.covid19.vaccinetracker.userrequests.model;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Dose preference
 */
public enum Dose {
    DOSE_1("1"),
    DOSE_2("2"),
    DOSE_BOTH("both");

    private static final Map<String, Dose> BY_LABEL = new HashMap<>();

    static {
        for (Dose d : values()) {
            BY_LABEL.put(d.dose, d);
        }
    }

    public static Dose find(String val) {
        Dose dose = BY_LABEL.get(val);
        return isNull(dose) ? null : dose;
    }

    private final String dose;

    Dose(String dose) {
        this.dose = dose;
    }

    @Override
    public String toString() {
        return dose;
    }
}
