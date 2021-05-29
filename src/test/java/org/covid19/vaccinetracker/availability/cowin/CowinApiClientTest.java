package org.covid19.vaccinetracker.availability.cowin;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;

import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class CowinApiClientTest {
    private final MockWebServer mockWebServer = new MockWebServer();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CowinConfig cowinConfig = new CowinConfig();
    private CowinApiClient cowinApiClient;

    @Mock
    private CowinApiAuth cowinApiAuth;

    @BeforeEach
    public void setup() {
        String url = String.format("http://localhost:%s", mockWebServer.getPort());
        cowinConfig.setApiUrl(url);
        cowinApiClient = new CowinApiClient(cowinConfig, cowinApiAuth);
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testFetchVaccineCentersByPincode() throws Exception {
        VaccineCenters expected = new VaccineCenters();
        expected.setCenters(Collections.singletonList(Center.builder().centerId(123).pincode(440022).districtName("Nagpur").build()));
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expected))
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        when(cowinApiAuth.isAvailable()).thenReturn(false);
        final VaccineCenters actual = cowinApiClient.fetchCentersByPincode("440022");

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void testFetchVaccineCentersByPincodeException() {
        VaccineCenters expected = new VaccineCenters();
        expected.setCenters(Collections.singletonList(Center.builder().centerId(123).pincode(440022).districtName("Nagpur").build()));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("random")
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        when(cowinApiAuth.isAvailable()).thenReturn(false);
        final VaccineCenters actual = cowinApiClient.fetchCentersByPincode("440022");
        Assertions.assertNull(actual);
    }

    @Test
    public void testFetchSessionsByDistrict() throws Exception {
        VaccineCenters expected = new VaccineCenters();
        expected.setCenters(Collections.singletonList(Center.builder().centerId(123).pincode(440022).districtName("Nagpur").build()));
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expected))
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        when(cowinApiAuth.isAvailable()).thenReturn(false);
        final VaccineCenters actual = cowinApiClient.fetchSessionsByDistrict(315);

        assertThat(actual, is(equalTo(expected)));
    }
}
