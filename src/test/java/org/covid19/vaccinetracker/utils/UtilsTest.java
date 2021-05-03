package org.covid19.vaccinetracker.utils;

import org.junit.jupiter.api.Test;

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
}
