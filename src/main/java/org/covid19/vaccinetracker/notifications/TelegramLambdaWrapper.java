package org.covid19.vaccinetracker.notifications;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.covid19.vaccinetracker.availability.aws.AWSConfig;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.util.Collections.emptyList;

@Slf4j
@Component
public class TelegramLambdaWrapper {
    private final AWSConfig awsConfig;
    private final AWSLambdaAsync awsLambdaAsync;
    private final ObjectMapper objectMapper;
    private final UserRequestManager userRequestManager;

    public TelegramLambdaWrapper(AWSConfig awsConfig, AWSLambdaAsync awsLambdaAsync, ObjectMapper objectMapper, UserRequestManager userRequestManager) {
        this.awsConfig = awsConfig;
        this.awsLambdaAsync = awsLambdaAsync;
        this.objectMapper = objectMapper;
        this.userRequestManager = userRequestManager;
    }

    /**
     * Invokes "SendTelegramMsg" Lambda asynchronously with given inputs
     *
     * @param chatId  - Id of the TG user
     * @param message - TG message
     */
    public void sendTelegramNotification(String chatId, String message) {
        createSendTelegramMsgLambdaEvent(chatId, message)
                .map(this::createSendTelegramMsgInvokeRequest)
                .ifPresent(invokeRequest -> awsLambdaAsync.invokeAsync(invokeRequest, sendTelegramMsgAsyncHandler()));
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
                        .ifPresent(response -> {
                            log.warn("Error sending TG notification to {}, error {}", response.getChatId(), response.getErrorMsg());
                            if (response.getErrorMsg().contains("bot was blocked by the user")
                                    || response.getErrorMsg().contains("user is deactivated")) {
                                // stop user preference to prevent further alerts being sent
                                userRequestManager.acceptUserRequest(response.getChatId(), emptyList());
                                log.warn("User {} pincode preferences cleared", response.getChatId());
                            }
                        });
            }
        };
    }

    @NotNull
    private Optional<SendTelegramMsgLambdaResponse> toSendTelegramMsgLambdaResponse(InvokeResult invokeResult) {
        return Stream.ofNullable(invokeResult.getPayload())
                .map(payload -> StandardCharsets.UTF_8.decode(payload).toString())
                .map(s -> Utils.parseLambdaResponseJson(objectMapper, s, SendTelegramMsgLambdaResponse.class))
                .findFirst();

    }

    private Optional<String> createSendTelegramMsgLambdaEvent(String chatId, String message) {
        try {
            return Optional.of(objectMapper.writeValueAsString(
                    SendTelegramMsgLambdaEvent.builder()
                            .chatId(chatId)
                            .message(message)
                            .build()));
        } catch (JsonProcessingException e) {
            log.error("Error serializing lambdaEvent for chatId {}", chatId);
            return Optional.empty();
        }
    }

    private InvokeRequest createSendTelegramMsgInvokeRequest(String event) {
        return new InvokeRequest()
                .withFunctionName(awsConfig.getSendTelegramMsgLambdaArn())
                .withPayload(event);
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
