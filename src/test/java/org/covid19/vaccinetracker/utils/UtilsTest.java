package org.covid19.vaccinetracker.utils;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineFee;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.covid19.vaccinetracker.utils.Utils.INDIA_TIMEZONE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
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
        assertTrue(Utils.dayOld(dtf.format(ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE)).minusHours(24))));
        assertFalse(Utils.dayOld(dtf.format(ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE)).minusHours(2))));
    }

    @Test
    public void testPast15Mins() {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        assertTrue(Utils.past15mins("2021-05-03T15:07:35.476954+05:30"));
        assertTrue(Utils.past15mins(dtf.format(ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE)).minusMinutes(30))));
        assertFalse(Utils.past15mins(dtf.format(ZonedDateTime.now(ZoneId.of(INDIA_TIMEZONE)).minusMinutes(10))));
    }

    @Test
    public void testNotificationText() {
        String expected = "<b>Premlok Park Disp- 2(18-44) (Pune 411033) - Paid</b>\n<pre>" +
                "\n8 doses (Dose 1: 3, Dose 2: 5) of COVISHIELD for 18+ age group available on 4th May for ₹780\n" +
                "(18+ आयु वर्ग के लिए COVISHIELD की 8 खुराकें (खुराक 1: 3, खुराक 2: 5) 4th May को उपलब्ध हैं)\n" +
                "\n15 doses (Dose 1: 12, Dose 2: 3) of COVAXIN for 18+ age group available on 5th May for ₹1410\n" +
                "(18+ आयु वर्ग के लिए COVAXIN की 15 खुराकें (खुराक 1: 12, खुराक 2: 3) 5th May को उपलब्ध हैं)\n</pre>\n" +
                "For registration, please visit <a href=\"https://selfregistration.cowin.gov.in/\">CoWIN Website</a>\n";
        List<Center> centers = new ArrayList<>();
        List<Session> sessions = new ArrayList<>();
        sessions.add(Session.builder().availableCapacity(8).availableCapacityDose1(3).availableCapacityDose2(5).minAgeLimit(18).date("04-05-2021").vaccine("COVISHIELD").cost("780").build());
        sessions.add(Session.builder().availableCapacity(15).availableCapacityDose1(12).availableCapacityDose2(3).minAgeLimit(18).date("05-05-2021").vaccine("COVAXIN").cost("1410").build());
        centers.add(Center.builder().name("Premlok Park Disp- 2(18-44)").districtName("Pune").pincode(411033).feeType("Paid").sessions(sessions)
                .vaccineFees(List.of(VaccineFee.builder().vaccine("COVISHIELD").fee("780").build(), VaccineFee.builder().vaccine("COVAXIN").build())).build());
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

    @Test
    public void testHumanReadableDate() {
        assertThat(Utils.humanReadable("12-01-2020"), is(equalTo("12th Jan")));
        assertThat(Utils.humanReadable("22-04-2021"), is(equalTo("22nd Apr")));
        assertThat(Utils.humanReadable("01-05-2021"), is(equalTo("1st May")));
        assertThat(Utils.humanReadable("10-06-2021"), is(equalTo("10th Jun")));
        assertThat(Utils.humanReadable("23-07-2021"), is(equalTo("23rd Jul")));
        assertThat(Utils.humanReadable("18-08-2021"), is(equalTo("18th Aug")));
    }
}

