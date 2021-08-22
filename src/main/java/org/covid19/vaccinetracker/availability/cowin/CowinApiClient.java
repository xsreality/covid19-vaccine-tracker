package org.covid19.vaccinetracker.availability.cowin;

import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class CowinApiClient {
    private final WebClient cowinClient;
    private final CowinApiAuth cowinApiAuth;

    private static final String PATH_CALENDAR_BY_PIN = "/v2/appointment/sessions/public/calendarByPin";
    private static final String PATH_CALENDAR_BY_PIN_AUTH = "/v2/appointment/sessions/calendarByPin";
    private static final String PATH_CALENDAR_BY_DISTRICT = "/v2/appointment/sessions/public/calendarByDistrict";
    private static final String PATH_CALENDAR_BY_DISTRICT_AUTH = "/v2/appointment/sessions/calendarByDistrict";

    public CowinApiClient(CowinConfig cowinConfig, CowinApiAuth cowinApiAuth) {
        this.cowinClient = WebClient
                .builder()
                .baseUrl(cowinConfig.getApiUrl())
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .filter(WebClientFilter.logRequest())
                .filter(WebClientFilter.logResponse())
                .build();
        this.cowinApiAuth = cowinApiAuth;
    }

    public VaccineCenters fetchCentersByPincode(String pincode) {
        try {
            if (cowinApiAuth.isAvailable()) {
                return cowinClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(PATH_CALENDAR_BY_PIN_AUTH)
                                .queryParam("pincode", "{pincode}")
                                .queryParam("date", "{date}")
                                .build(pincode, Utils.tomorrowIST()))
                        .headers(h -> h.setBearerAuth(cowinApiAuth.getBearerToken()))
                        .headers(h -> h.setAccept(singletonList(APPLICATION_JSON)))
                        .retrieve()
                        .bodyToMono(VaccineCenters.class)
                        .block();
            } else {
                return cowinClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(PATH_CALENDAR_BY_PIN)
                                .queryParam("pincode", "{pincode}")
                                .queryParam("date", "{date}")
                                .build(pincode, Utils.todayIST()))
                        .retrieve()
                        .bodyToMono(VaccineCenters.class)
                        .block();
            }
        } catch (CowinException we) {
            log.error("Error from Cowin API for pincode {} status code {}, message {}", pincode, we.getStatusCode(), we.getMessage());
            return null;
        }
    }

    public VaccineCenters fetchSessionsByDistrict(int districtId) {
        try {
            if (cowinApiAuth.isAvailable()) {
                return cowinClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(PATH_CALENDAR_BY_DISTRICT_AUTH)
                                .queryParam("district_id", "{district_id}")
                                .queryParam("date", "{date}")
                                .build(districtId, Utils.todayIST()))
                        .headers(h -> h.setBearerAuth(cowinApiAuth.getBearerToken()))
                        .headers(h -> h.setAccept(singletonList(APPLICATION_JSON)))
                        .retrieve()
                        .bodyToMono(VaccineCenters.class)
                        .block();
            } else {
                return cowinClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(PATH_CALENDAR_BY_DISTRICT)
                                .queryParam("district_id", "{district_id}")
                                .queryParam("date", "{date}")
                                .build(districtId, Utils.todayIST()))
                        .retrieve()
                        .bodyToMono(VaccineCenters.class)
                        .block();
            }
        } catch (CowinException we) {
            log.error("Error from Cowin API for district {} status code {}, message {}", districtId, we.getStatusCode(), we.getMessage());
            return null;
        }
    }

    public boolean isProtected() {
        return this.cowinApiAuth.isAvailable();
    }
}
