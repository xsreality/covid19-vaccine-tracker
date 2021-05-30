package org.covid19.vaccinetracker.userrequests.model;

/**
 * Age preference
 */
public enum Age {
    AGE_18_44("18-44"),
    AGE_45("45+"),
    AGE_BOTH("both");

    private final String age;

    Age(String age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return age;
    }
}
