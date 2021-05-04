package org.covid19.vaccinetracker.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilsTest {

    @Test
    public void pincodeValidationTest() {
        assertTrue(Utils.allValidPincodes("440022"), "Should be valid!");
        assertTrue(Utils.allValidPincodes("400008"), "Should be valid!");
        assertTrue(Utils.allValidPincodes("410038"), "Should be valid!");
        assertFalse(Utils.allValidPincodes("4561091"), "Should not be valid!");
        assertFalse(Utils.allValidPincodes("40010"), "Should not be valid!");
        assertFalse(Utils.allValidPincodes("abcde"), "Should not be valid!");
        assertFalse(Utils.allValidPincodes("395def"), "Should not be valid!");
    }

    @Test
    public void testDayOldDate() {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        assertTrue(Utils.dayOld("2021-05-03T15:07:35.476954+05:30"));
        assertTrue(Utils.dayOld(dtf.format(ZonedDateTime.now().minusHours(24))));
        assertFalse(Utils.dayOld(dtf.format(ZonedDateTime.now().minusHours(2))));
    }
}
