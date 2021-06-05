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

import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.notifications.KafkaNotifications;
import org.covid19.vaccinetracker.notifications.VaccineCentersProcessor;
import org.covid19.vaccinetracker.utils.Utils;
import org.jetbrains.annotations.NotNull;
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
    private final KafkaNotifications kafkaNotifications;
    private final ExecutorService districtsProcessorExecutor;

    public CowinLambdaWrapper(AWSConfig awsConfig, AWSLambda awsLambda, AWSLambdaAsync awsLambdaAsync,
                              ObjectMapper objectMapper, VaccineCentersProcessor vaccineCentersProcessor, KafkaNotifications kafkaNotifications) {
        this.awsConfig = awsConfig;
        this.awsLambda = awsLambda;
        this.awsLambdaAsync = awsLambdaAsync;
        this.objectMapper = objectMapper;
        this.vaccineCentersProcessor = vaccineCentersProcessor;
        this.kafkaNotifications = kafkaNotifications;
        this.districtsProcessorExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("vaccinelambda-%d").build());
    }

    /**
     * Invokes "SendTelegramMsg" Lambda asynchronously with given inputs
     *
     * @param chatId  - Id of the TG user
     * @param message - TG message
     */
    public void sendTelegramNotification(String chatId, String message) {
        Stream.ofNullable(createSendTelegramMsgLambdaEvent(chatId, message))
                .map(this::createSendTelegramMsgInvokeRequest)
                .forEach(invokeRequest -> awsLambdaAsync.invokeAsync(invokeRequest, sendTelegramMsgAsyncHandler()));
    }

    @NotNull
    private AsyncHandler<InvokeRequest, InvokeResult> sendTelegramMsgAsyncHandler() {
        return new AsyncHandler<>() {
            @Override
            public void onError(Exception e) {
                log.error("Got error {}", e.getMessage());
            }

            @Override
            public void onSuccess(InvokeRequest request, InvokeResult result) {
                toSendTelegramMsgLambdaResponse(result)
                        .filter(response -> !response.getStatus())
                        .ifPresent(response -> log.warn("Error sending TG notification to {}, error {}", response.getChatId(), response.getErrorMsg()));
            }
        };
    }

    @NotNull
    private Optional<SendTelegramMsgLambdaResponse> toSendTelegramMsgLambdaResponse(InvokeResult invokeResult) {
        return Stream.ofNullable(invokeResult.getPayload())
                .map(payload -> StandardCharsets.UTF_8.decode(payload).toString())
                .map(s -> parseLambdaResponseJson(s, SendTelegramMsgLambdaResponse.class))
                .findFirst();

    }

    private String createSendTelegramMsgLambdaEvent(String chatId, String message) {
        try {
            return objectMapper.writeValueAsString(
                    SendTelegramMsgLambdaEvent.builder()
                            .chatId(chatId)
                            .message(message)
                            .build()
            );
        } catch (JsonProcessingException e) {
            log.error("Error serializing lambdaEvent for chatId {}", chatId);
            return null;
        }
    }

    private InvokeRequest createSendTelegramMsgInvokeRequest(String event) {
        return new InvokeRequest()
                .withFunctionName(awsConfig.getSendTelegramMsgArn())
                .withPayload(event);
    }

    /**
     * Invokes "CalendarByDistrict" Lambda asynchronously with given inputs
     *
     * @param districtId - Id of the District
     */
    public void processDistrict(int districtId) {
        Stream.ofNullable(createCalendarByDistrictLambdaEvent(districtId))
                .map(this::createCalendarByDistrictInvokeRequest)
                .forEach(invokeRequest -> awsLambdaAsync.invokeAsync(invokeRequest, calendarByDistrictAsyncHandler()));
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
                                    vaccineCentersProcessor.persistVaccineCenters(vaccineCenters); // DB
                                    kafkaNotifications.sendUpdatedPincodesToKafka(vaccineCenters); // Kafka
                                    log.debug("Processing completed.");
                                })
                );
            }
        };
    }

    public Stream<Optional<VaccineCenters>> fetchSessionsByPincode(String pincode) {
        return Stream.ofNullable(createCalendarByPinLambdaEvent(pincode))
                .map(this::createCalendarByPinInvokeRequest)
                .map(awsLambda::invoke)
                .map(this::toVaccineCenters);
    }

    @NotNull
    private Optional<VaccineCenters> toVaccineCenters(InvokeResult invokeResult) {
        return Stream.ofNullable(invokeResult.getPayload())
                .map(payload -> StandardCharsets.UTF_8.decode(payload).toString())
                .map(s -> parseLambdaResponseJson(s, CalendarByDistrictLambdaResponse.class))
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

    private <T> T parseLambdaResponseJson(String responseJson, Class<T> clazz) {
        try {
            return objectMapper.readValue(responseJson, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error parsing response from Lambda: {}", e.getMessage());
            return null;
        }
    }

    private String createCalendarByDistrictLambdaEvent(int districtId) {
        try {
            return objectMapper.writeValueAsString(
                    CalendarByDistrictLambdaEvent.builder()
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

    private String createCalendarByPinLambdaEvent(String pincode) {
        try {
            return objectMapper.writeValueAsString(
                    CalendarByPinLambdaEvent.builder()
                            .pincode(pincode)
                            .date(Utils.todayIST())
                            .build()
            );
        } catch (JsonProcessingException e) {
            log.error("Error serializing lambdaEvent for pincode {}", pincode);
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
class SendTelegramMsgLambdaEvent {
    @JsonProperty("chat_id")
    private String chatId;
    private String message;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class SendTelegramMsgLambdaResponse {
    @JsonProperty("chat_id")
    private String chatId;
    private Boolean status;
    @JsonProperty("error_msg")
    private String errorMsg;
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
