package org.covid19.vaccinetracker.availability.cowin;

import org.covid19.vaccinetracker.availability.model.ConfirmOtpResponse;
import org.covid19.vaccinetracker.availability.model.GenerateOtpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import static java.util.Map.entry;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class CowinApiOtpClient {
    private final WebClient cowinClient;

    public CowinApiOtpClient(CowinConfig cowinConfig) {
        this.cowinClient = WebClient
                .builder()
                .baseUrl(cowinConfig.getApiUrl())
                .filter(WebClientFilter.logRequest())
                .filter(WebClientFilter.logResponse())
                .build();
    }

    public GenerateOtpResponse generateOtp(String mobileNumber) {
        Map<String, String> body = Map.ofEntries(entry("mobile", mobileNumber));
        try {
            return cowinClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/auth/generateOTP")
                            .build())
                    .body(BodyInserters.fromValue(body))
                    .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(GenerateOtpResponse.class)
                    .block();
        } catch (CowinException we) {
            log.error("Error from Cowin API when generating OTP status code {}, message {}", we.getStatusCode(), we.getMessage());
            return null;
        }
    }

    public ConfirmOtpResponse confirmOtp(String transactionId, String otp) {
        Map<String, String> body = Map.ofEntries(
                entry("txnId", transactionId),
                entry("otp", otp)
        );
        try {
            return cowinClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/auth/confirmOTP")
                            .build())
                    .body(BodyInserters.fromValue(body))
                    .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(ConfirmOtpResponse.class)
                    .block();
        } catch (CowinException we) {
            log.error("Error from Cowin API when confirming OTP status code {}, message {}", we.getStatusCode(), we.getMessage());
            return null;
        }
    }
}
