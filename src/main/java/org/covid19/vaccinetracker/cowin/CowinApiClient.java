package org.covid19.vaccinetracker.cowin;

import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CowinApiClient {
    private final WebClient cowinClient;
    private final CowinConfig cowinConfig;

    public CowinApiClient(CowinConfig cowinConfig) {
        this.cowinConfig = cowinConfig;
        this.cowinClient = WebClient
                .builder()
                .baseUrl(cowinConfig.getApiUrl())
                .filter(WebClientFilter.logRequest())
                .filter(WebClientFilter.logResponse())
                .build();
    }

    public VaccineCenters fetchCentersByPincode(String pincode) {
        try {
            return cowinClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("pincode", "{pincode}")
                            .queryParam("date", "{date}")
                            .build(pincode, Utils.todayIST()))
                    .retrieve()
                    .bodyToMono(VaccineCenters.class)
                    .block();
        } catch (WebClientResponseException we) {
            throw new CowinException(we.getMessage(), we.getRawStatusCode());
        }
    }
}
