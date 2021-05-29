package org.covid19.vaccinetracker.userrequests;

import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.userrequests.model.State;
import org.covid19.vaccinetracker.persistence.mariadb.repository.DistrictRepository;
import org.covid19.vaccinetracker.persistence.mariadb.repository.PincodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class MetadataStoreTest {
    @Autowired
    private DistrictRepository districtRepository;
    @Autowired
    private PincodeRepository pincodeRepository;

    private MetadataStore metadataStore;

    @BeforeEach
    public void setup() {
        this.metadataStore = new MetadataStore(districtRepository, pincodeRepository);
    }

    @Test
    public void testPincodeExists() {
        assertTrue(metadataStore.pincodeExists("127310"));
        assertFalse(metadataStore.pincodeExists("440017"));
    }

    @Test
    public void testFetchDistrictByNameAndState() {
        final District expected = new District(201, "Charkhi Dadri", new State(12, "Haryana"));
        assertEquals(expected, metadataStore.fetchDistrictByNameAndState("Charkhi Dadri", "Haryana"));
    }

    @Test
    public void testFetchDistrictsByPincode() {
        final District expected = new District(201, "Charkhi Dadri", new State(12, "Haryana"));
        assertEquals(singletonList(expected), metadataStore.fetchDistrictsByPincode("127310"));
    }
}
