package org.covid19.vaccinetracker.userrequests.model;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Vaccine preference
 */
public enum Vaccine {
    COVISHIELD("Covishield"),
    COVAXIN("Covaxin"),
    SPUTNIK_V("Sputnik V"),
    ALL("All");

    private static final Map<String, Vaccine> BY_LABEL = new HashMap<>();

    static {
        for (Vaccine v : values()) {
            BY_LABEL.put(v.vaccine, v);
        }
    }

    public static Vaccine find(String val) {
        Vaccine vaccine = BY_LABEL.get(val);
        return isNull(vaccine) ? null : vaccine;
    }

    private final String vaccine;

    Vaccine(String vaccine) {
        this.vaccine = vaccine;
    }

    @Override
    public String toString() {
        return vaccine;
    }
}
