package org.covid19.vaccinetracker.userrequests.model;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Age preference
 */
public enum Age {
    AGE_18_44("18-44"),
    AGE_45("45+"),
    AGE_BOTH("both");

    private static final Map<String, Age> BY_LABEL = new HashMap<>();

    static {
        for (Age a : values()) {
            BY_LABEL.put(a.age, a);
        }
    }

    public static Age find(String val) {
        Age age = BY_LABEL.get(val);
        return isNull(age) ? null : age;
    }

    private final String age;

    Age(String age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return age;
    }
}
