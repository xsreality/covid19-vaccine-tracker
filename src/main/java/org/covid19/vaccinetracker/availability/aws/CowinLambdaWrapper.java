package org.covid19.vaccinetracker.availability.aws;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CowinLambdaWrapper implements DisposableBean {
    @Value("${topic.updated.pincodes}")
    private String updatedPincodesTopic;

    private final AWSConfig awsConfig;
    private final AWSLambda awsLambda;
    private final AWSLambdaAsync awsLambdaAsync;
    private final ObjectMapper objectMapper;
    private final VaccinePersistence vaccinePersistence;
    private final KafkaTemplate<String, String> updatedPincodesKafkaTemplate;
    private final ExecutorService districtsProcessorExecutor;

    public CowinLambdaWrapper(AWSConfig awsConfig, AWSLambda awsLambda, AWSLambdaAsync awsLambdaAsync,
                              ObjectMapper objectMapper, VaccinePersistence vaccinePersistence, KafkaTemplate<String, String> updatedPincodesKafkaTemplate) {
        this.awsConfig = awsConfig;
        this.awsLambda = awsLambda;
        this.awsLambdaAsync = awsLambdaAsync;
        this.objectMapper = objectMapper;
        this.vaccinePersistence = vaccinePersistence;
        this.updatedPincodesKafkaTemplate = updatedPincodesKafkaTemplate;
        this.districtsProcessorExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("vaccinelambda-%d").build());
    }

    /**
     * Invokes "CalendarByDistrict" Lambda asynchronously with given inputs
     *
     * @param districtId - Id of the District
     */
    public void processDistrict(int districtId) {
        createCalendarByDistrictLambdaEvent(districtId)
                .map(this::createCalendarByDistrictInvokeRequest)
                .ifPresent(invokeRequest -> awsLambdaAsync.invokeAsync(invokeRequest, calendarByDistrictAsyncHandler()));
    }

    @NotNull
    private AsyncHandler<InvokeRequest, InvokeResult> calendarByDistrictAsyncHandler() {
        return new AsyncHandler<>() {
            @Override
            public void onError(Exception e) {
                log.error("Got error {}", e.getMessage());
            }

            @Override
            public void onSuccess(InvokeRequest request, InvokeResult result) {
                // run in separate thread to not delay Lambda callback thread
                districtsProcessorExecutor.submit(() ->
                        toVaccineCenters(result)
                                .stream()
                                .filter(Objects::nonNull)
                                .forEach(vaccineCenters -> {
                                    vaccinePersistence.persistVaccineCenters(vaccineCenters); // DB
                                    sendUpdatedPincodesToKafka(vaccineCenters); // Kafka
                                    log.debug("Processing completed.");
                                })
                );
            }
        };
    }

    public void sendUpdatedPincodesToKafka(VaccineCenters vaccineCenters) {
        vaccineCenters.getCenters()
                .stream()
                .filter(Center::areVaccineCentersAvailableFor18plus)
                .map(Center::getPincode)
                .map(String::valueOf)
                .distinct()
                .forEach(pincode -> updatedPincodesKafkaTemplate.send(updatedPincodesTopic, pincode, pincode));
    }

    public Stream<Optional<VaccineCenters>> fetchSessionsByPincode(String pincode) {
        return createCalendarByPinLambdaEvent(pincode)
                .map(this::createCalendarByPinInvokeRequest)
                .map(awsLambda::invoke)
                .map(this::toVaccineCenters)
                .stream()
                ;
    }

    @NotNull
    private Optional<VaccineCenters> toVaccineCenters(InvokeResult invokeResult) {
        return Stream.ofNullable(invokeResult.getPayload())
                .map(payload -> StandardCharsets.UTF_8.decode(payload).toString())
                .map(s -> Utils.parseLambdaResponseJson(objectMapper, s, CalendarByDistrictLambdaResponse.class))
                .filter(Objects::nonNull)
                .peek(this::logIfInvalidStatusCode)
                .filter(this::statusCode200)
                .map(CalendarByDistrictLambdaResponse::getPayload)
                .findFirst();
    }

    private boolean statusCode200(CalendarByDistrictLambdaResponse calendarByDistrictLambdaResponse) {
        return "200".equals(calendarByDistrictLambdaResponse.getStatusCode());
    }

    private InvokeRequest createCalendarByDistrictInvokeRequest(String event) {
        return new InvokeRequest()
                .withFunctionName(awsConfig.getCalendarByDistrictLambdaArn())
                .withPayload(event);
    }

    private InvokeRequest createCalendarByPinInvokeRequest(String event) {
        return new InvokeRequest()
                .withFunctionName(awsConfig.getCalendarByPinLambdaArn())
                .withPayload(event);
    }

    private void logIfInvalidStatusCode(CalendarByDistrictLambdaResponse calendarByDistrictLambdaResponse) {
        if (!"200".equals(calendarByDistrictLambdaResponse.getStatusCode())) {
            log.info("Got invalid status code {} for district {}", calendarByDistrictLambdaResponse.getStatusCode(), calendarByDistrictLambdaResponse.getDistrictId());
        }
    }

    private Optional<String> createCalendarByDistrictLambdaEvent(int districtId) {
        try {
            return Optional.of(objectMapper.writeValueAsString(
                    CalendarByDistrictLambdaEvent.builder()
                            .districtId(String.valueOf(districtId))
                            .date(Utils.todayIST())
                            .bearerToken("")
                            .build()));
        } catch (JsonProcessingException e) {
            log.error("Error serializing lambdaEvent for district {}", districtId);
            return Optional.empty();
        }
    }

    private Optional<String> createCalendarByPinLambdaEvent(String pincode) {
        try {
            return Optional.of(objectMapper.writeValueAsString(
                    CalendarByPinLambdaEvent.builder()
                            .pincode(pincode)
                            .date(Utils.todayIST())
                            .build()));
        } catch (JsonProcessingException e) {
            log.error("Error serializing lambdaEvent for pincode {}", pincode);
            return Optional.empty();
        }
    }

    @Override
    public void destroy() {
        this.districtsProcessorExecutor.shutdown();
        try {
            if (!districtsProcessorExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                districtsProcessorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            districtsProcessorExecutor.shutdownNow();
        }
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
class CalendarByDistrictLambdaEvent {
    @JsonProperty("district_id")
    private String districtId;
    private String date;
    @JsonProperty("bearer_token")
    private String bearerToken;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class CalendarByDistrictLambdaResponse {
    @JsonProperty("status_code")
    private String statusCode;
    private VaccineCenters payload;
    @JsonProperty("district_id")
    private String districtId;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
class CalendarByPinLambdaEvent {
    private String pincode;
    private String date;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class CalendarByPinLambdaResponse {
    @JsonProperty("status_code")
    private String statusCode;
    private VaccineCenters payload;
    private String pincode;
}
