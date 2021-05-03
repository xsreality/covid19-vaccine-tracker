package org.covid19.vaccinetracker.cowin;

import org.covid19.vaccinetracker.model.VaccineCenters;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
public class CowinApiClient {

    private final WebClient cowinClient;
    private final CowinConfig cowinConfig;

    public CowinApiClient(CowinConfig cowinConfig) {
        this.cowinConfig = cowinConfig;
        this.cowinClient = WebClient.create(cowinConfig.getApiUrl());
    }

    public VaccineCenters fetchCentersByPincode(String pincode) {
        final Mono<VaccineCenters> response = cowinClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("pincode", "{pincode}")
                        .queryParam("date", "{date}")
                        .build(pincode, "04-05-2021"))
                .retrieve()
                .bodyToMono(VaccineCenters.class).log();
        return response.block();
    }
}
