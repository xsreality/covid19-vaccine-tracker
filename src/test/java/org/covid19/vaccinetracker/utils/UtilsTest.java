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
        String expected = "<b>Premlok Park Disp- 2(18-44) (Pune 411033)</b>\n<pre>" +
                "\n3 doses of COVISHIELD for 18+ age group available on 04-05-2021 (18+ आयु वर्ग के लिए COVISHIELD की 3 खुराकें 04-05-2021 को उपलब्ध हैं)\n" +
                "\n12 doses of COVAXIN for 18+ age group available on 05-05-2021 (18+ आयु वर्ग के लिए COVAXIN की 12 खुराकें 05-05-2021 को उपलब्ध हैं)\n</pre>\n" +
                "For registration, please visit <a href=\"https://selfregistration.cowin.gov.in/\">CoWIN Website</a>\n";
        List<Center> centers = new ArrayList<>();
        List<Session> sessions = new ArrayList<>();
        sessions.add(Session.builder().availableCapacity(6).availableCapacityDose1(3).availableCapacityDose2(5).minAgeLimit(18).date("04-05-2021").vaccine("COVISHIELD").build());
        sessions.add(Session.builder().availableCapacity(4).availableCapacityDose1(12).availableCapacityDose2(3).minAgeLimit(18).date("05-05-2021").vaccine("COVAXIN").build());
        centers.add(Center.builder().name("Premlok Park Disp- 2(18-44)").districtName("Pune").pincode(411033).sessions(sessions).build());
        assertEquals(expected, Utils.buildNotificationMessage(centers), "Notification text does not match");
    }

    @Test
    public void testIsValidJwtToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiJlYWYxMDc3Ny00ZjIxLTQwNjYtYTQ2Yy0yNDhjOGJkNzNiNDEiLCJ1c2VyX2lkIjoiZWFmMTA3NzctNGYyMS00MDY2LWE0NmMtMjQ4YzhiZDczYjQxIiwidXNlcl90eXBlIjoiQkVORUZJQ0lBUlkiLCJtb2JpbGVfbnVtYmVyIjo5OTk5OTk5OTk5LCJiZW5lZmljaWFyeV9yZWZlcmVuY2VfaWQiOjc1MTIxOTk5ODE4MTUwLCJ1YSI6Imluc29tbmlhLzIwMjEuMy4wIiwiZGF0ZV9tb2RpZmllZCI6IjIwMjEtMDUtMTVUMTA6MTg6MzkuMjk5WiIsImlhdCI6MTYyMTA3MzkxOSwiZXhwIjoxNjIxMDc0ODE5fQ.YDQCfcSwtKAT5epVMOFt3jmQ1dc6jOZ-XbYISOmQLGw";
        assertFalse(Utils.isValidJwtToken(invalidToken));

        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiJlYWYxMDc3Ny00ZjIxLTQwNjYtYTQ2Yy0yNDhjOGJkNzNiNDEiLCJ1c2VyX2lkIjoiZWFmMTA3NzctNGYyMS00MDY2LWE0NmMtMjQ4YzhiZDczYjQxIiwidXNlcl90eXBlIjoiQkVORUZJQ0lBUlkiLCJtb2JpbGVfbnVtYmVyIjo5OTk5OTk5OTk5LCJiZW5lZmljaWFyeV9yZWZlcmVuY2VfaWQiOjc1MTIxOTk5ODE4MTUwLCJ1YSI6Imluc29tbmlhLzIwMjEuMy4wIiwiZGF0ZV9tb2RpZmllZCI6IjIwMjEtMDUtMTZUMTA6MTg6MzkuMjk5WiIsImlhdCI6MTYyMTA3MzkxOSwiZXhwIjoyNjIxMTc0ODE5fQ.CDXX9uaPoz6dk19EvYwZC_UkJYRJA_z2FSVYSXzTPyc";
        assertTrue(Utils.isValidJwtToken(validToken));

        String emptyToken = "";
        assertFalse(Utils.isValidJwtToken(emptyToken));
        assertFalse(Utils.isValidJwtToken(null));
    }
}

