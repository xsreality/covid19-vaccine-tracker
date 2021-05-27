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

import org.covid19.vaccinetracker.availability.VaccineCentersProcessor;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
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
    private final AWSConfig awsConfig;
    private final AWSLambda awsLambda;
    private final AWSLambdaAsync awsLambdaAsync;
    private final ObjectMapper objectMapper;
    private final VaccineCentersProcessor vaccineCentersProcessor;
    private final ExecutorService districtsProcessorExecutor;

    public CowinLambdaWrapper(AWSConfig awsConfig, AWSLambda awsLambda, AWSLambdaAsync awsLambdaAsync,
                              ObjectMapper objectMapper, VaccineCentersProcessor vaccineCentersProcessor) {
        this.awsConfig = awsConfig;
        this.awsLambda = awsLambda;
        this.awsLambdaAsync = awsLambdaAsync;
        this.objectMapper = objectMapper;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.districtsProcessorExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("vaccinelambda-%d").build());
    }

    public void processDistrict(int districtId) {
        Stream.ofNullable(createLambdaEvent(districtId))
                .map(this::createInvokeRequest)
                .forEach(invokeRequest -> awsLambdaAsync.invokeAsync(invokeRequest, asyncHandler()));
    }

    @NotNull
    private AsyncHandler<InvokeRequest, InvokeResult> asyncHandler() {
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
                                    vaccineCentersProcessor.persistVaccineCenters(vaccineCenters); // DB
                                    vaccineCentersProcessor.sendUpdatedPincodesToKafka(vaccineCenters); // Kafka
                                    log.debug("Processing completed.");
                                })
                );
            }
        };
    }

    public Stream<Optional<VaccineCenters>> fetchSessionsByDistrict(int districtId) {
        return Stream.ofNullable(createLambdaEvent(districtId))
                .map(this::createInvokeRequest)
                .map(awsLambda::invoke)
                .map(this::toVaccineCenters);
    }

    @NotNull
    private Optional<VaccineCenters> toVaccineCenters(InvokeResult invokeResult) {
        return Stream.ofNullable(invokeResult.getPayload())
                .map(payload -> StandardCharsets.UTF_8.decode(payload).toString())
                .map(this::parseLambdaResponseJson)
                .filter(Objects::nonNull)
                .peek(this::logIfInvalidStatusCode)
                .filter(this::statusCode200)
                .map(LambdaResponse::getPayload)
                .findFirst();
    }

    private boolean statusCode200(LambdaResponse lambdaResponse) {
        return "200".equals(lambdaResponse.getStatusCode());
    }

    private InvokeRequest createInvokeRequest(String event) {
        return new InvokeRequest()
                .withFunctionName(awsConfig.getLambdaFunctionArn())
                .withPayload(event);
    }

    private void logIfInvalidStatusCode(LambdaResponse lambdaResponse) {
        if (!"200".equals(lambdaResponse.getStatusCode())) {
            log.info("Got invalid status code {} for district {}", lambdaResponse.getStatusCode(), lambdaResponse.getDistrictId());
        }
    }

    @Nullable
    private LambdaResponse parseLambdaResponseJson(String responseJson) {
        try {
            return objectMapper.readValue(responseJson, LambdaResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing response from Lambda: {}", e.getMessage());
            return null;
        }
    }

    private String createLambdaEvent(int districtId) {
        try {
            return objectMapper.writeValueAsString(
                    LambdaEvent.builder()
                            .districtId(String.valueOf(districtId))
                            .date(Utils.todayIST())
                            .bearerToken("")
                            .build()
            );
        } catch (JsonProcessingException e) {
            log.error("Error serializing lambdaEvent for district {}", districtId);
            return null;
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
class LambdaEvent {
    @JsonProperty("district_id")
    private String districtId;
    private String date;
    @JsonProperty("bearer_token")
    private String bearerToken;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class LambdaResponse {
    @JsonProperty("status_code")
    private String statusCode;
    private VaccineCenters payload;
    @JsonProperty("district_id")
    private String districtId;
}
