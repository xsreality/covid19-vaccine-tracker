package org.covid19.vaccinetracker.cowin;

import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CowinApiClient {
    private final WebClient cowinClient;

    public CowinApiClient(CowinConfig cowinConfig) {
        this.cowinClient = WebClient
                .builder()
                .baseUrl(cowinConfig.getApiUrl())
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .filter(WebClientFilter.logRequest())
                .filter(WebClientFilter.logResponse())
                .build();
    }

    public VaccineCenters fetchCentersByPincode(String pincode) {
        try {
            return cowinClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("v2/appointment/sessions/public/calendarByPin")
                            .queryParam("pincode", "{pincode}")
                            .queryParam("date", "{date}")
                            .build(pincode, Utils.todayIST()))
                    .retrieve()
                    .bodyToMono(VaccineCenters.class)
                    .block();
        } catch (CowinException we) {
            log.error("Error from Cowin API for pincode {} status code {}, message {}", pincode, we.getStatusCode(), we.getMessage());
            return null;
        }
    }

    public VaccineCenters fetchSessionsByDistrict(int districtId) {
        try {
            return cowinClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("v2/appointment/sessions/public/calendarByDistrict")
                            .queryParam("district_id", "{district_id}")
                            .queryParam("date", "{date}")
                            .build(districtId, Utils.todayIST()))
                    .retrieve()
                    .bodyToMono(VaccineCenters.class)
                    .block();
        } catch (CowinException we) {
            log.error("Error from Cowin API for district {} status code {}, message {}", districtId, we.getStatusCode(), we.getMessage());
            return null;
        }
    }
}
