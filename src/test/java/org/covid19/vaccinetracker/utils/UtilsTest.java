package org.covid19.vaccinetracker.utils;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    public void testNotificationText() {
        String expected = "Premlok Park Disp- 2(18-44) (Pune 411033)\n" +
                "3 dose(s) of COVISHIELD for 18+ age group available on 04-05-2021\n" +
                "12 dose(s) of COVAXIN for 18+ age group available on 05-05-2021\n\n";
        List<Center> centers = new ArrayList<>();
        List<Session> sessions = new ArrayList<>();
        sessions.add(Session.builder().availableCapacity(3).minAgeLimit(18).date("04-05-2021").vaccine("COVISHIELD").build());
        sessions.add(Session.builder().availableCapacity(12).minAgeLimit(18).date("05-05-2021").vaccine("COVAXIN").build());
        centers.add(Center.builder().name("Premlok Park Disp- 2(18-44)").districtName("Pune").pincode(411033).sessions(sessions).build());
        assertEquals(expected, Utils.buildNotificationMessage(centers), "Notification text does not match");
    }
}

