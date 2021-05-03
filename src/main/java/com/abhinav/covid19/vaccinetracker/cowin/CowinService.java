package com.abhinav.covid19.vaccinetracker.cowin;

import com.abhinav.covid19.vaccinetracker.VaccineCenters;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class CowinService {

    private final WebClient cowinClient;
    private final CowinConfig cowinConfig;

    public CowinService(CowinConfig cowinConfig) {
        this.cowinConfig = cowinConfig;
        this.cowinClient = WebClient.create(cowinConfig.getApiUrl());
    }

    public VaccineCenters fetchCentersByPincode(String pincode) {
        final Mono<VaccineCenters> response = cowinClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("pincode", "{pinecode}")
                        .queryParam("date", "{date}")
                        .build(pincode, "03-05-2021"))
                .retrieve()
                .bodyToMono(VaccineCenters.class).log();
        return response.block();
    }
}
