package org.covid19.vaccinetracker.availability.cowin;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.covid19.vaccinetracker.availability.model.ConfirmOtpResponse;
import org.covid19.vaccinetracker.availability.model.GenerateOtpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class CowinApiOtpClientTest {
    private final MockWebServer mockWebServer = new MockWebServer();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CowinConfig cowinConfig = new CowinConfig();
    private CowinApiOtpClient cowinApiOtpClient;

    @BeforeEach
    public void setup() {
        String url = String.format("http://localhost:%s", mockWebServer.getPort());
        cowinConfig.setApiUrl(url);
        cowinApiOtpClient = new CowinApiOtpClient(cowinConfig);
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testGenerateOtp() throws Exception {
        GenerateOtpResponse expected = GenerateOtpResponse.builder().transactionId("6171c423-90db-4b16-a3b1-bbd85d3cde6a").build();
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expected))
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        final GenerateOtpResponse actual = cowinApiOtpClient.generateOtp("9999999999");
        assertThat(actual, is(equalTo(expected)));
        final RecordedRequest recordedRequest = mockWebServer.takeRequest(3, SECONDS);
        assertThat(recordedRequest.getBody().readUtf8(), is(equalTo("{\"mobile\":\"9999999999\"}")));
        assertThat(recordedRequest.getHeader(CONTENT_TYPE), is(equalTo(APPLICATION_JSON_VALUE)));
    }

    @Test
    public void testConfirmOtp() throws Exception {
        ConfirmOtpResponse expected = ConfirmOtpResponse.builder().token("xxxxx").isNewAccount("N").build();
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expected))
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        final ConfirmOtpResponse actual = cowinApiOtpClient.confirmOtp(
                "62b136fa-831d-4132-8457-b7bc10916d9d",
                "deebfa8af67efe975b1859f418e90dcaad5875301fb6881fbea47b68f94432cd");
        assertThat(actual, is(equalTo(expected)));
        final RecordedRequest recordedRequest = mockWebServer.takeRequest(3, SECONDS);
        assertThat(recordedRequest.getHeader(CONTENT_TYPE), is(equalTo(APPLICATION_JSON_VALUE)));
    }
}
