package org.covid19.vaccinetracker.availability.aws;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.covid19.vaccinetracker.cowin.CowinApiAuth;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.nonNull;

@Slf4j
@Component
public class CowinLambdaClient {
    private final AWSConfig awsConfig;
    private final AWSLambda awsLambda;
    private final CowinApiAuth cowinApiAuth;
    private final ObjectMapper objectMapper;

    public CowinLambdaClient(AWSConfig awsConfig, AWSLambda awsLambda, CowinApiAuth cowinApiAuth, ObjectMapper objectMapper) {
        this.awsConfig = awsConfig;
        this.awsLambda = awsLambda;
        this.cowinApiAuth = cowinApiAuth;
        this.objectMapper = objectMapper;
    }

    public VaccineCenters fetchSessionsByDistrict(int districtId) {
        String lambdaEvent = String.format("{\n" +
                " \"district_id\": \"%d\",\n" +
                " \"date\": \"%s\"\n" +
                " \"bearer_token\": \"%s\"\n" +
                "}", districtId, Utils.todayIST(), cowinApiAuth.getBearerToken());



        return Stream.of(lambdaEvent)
                .map(event -> new InvokeRequest()
                        .withFunctionName(awsConfig.getLambdaFunctionArn())
                        .withPayload(event))
                .map(awsLambda::invoke)
                .filter(invokeResult -> nonNull(invokeResult.getPayload()))
                .map(invokeResult -> StandardCharsets.UTF_8.decode(invokeResult.getPayload()).toString())
                .map(responseJson -> {
                    try {
                        return objectMapper.readValue(responseJson, LambdaResponse.class);
                    } catch (JsonProcessingException e) {
                        log.error("Error parsing response from Lambda: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .peek(lambdaResponse -> {
                    if (!"200".equals(lambdaResponse.getStatusCode())) {
                        log.info("Got invalid status code: {}", lambdaResponse.getStatusCode());
                    }
                })
                .filter(lambdaResponse -> "200".equals(lambdaResponse.getStatusCode()))
                .map(LambdaResponse::getPayload)
                .findFirst()
                .orElse(null);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class LambdaResponse {
    private String statusCode;
    private VaccineCenters payload;
}
