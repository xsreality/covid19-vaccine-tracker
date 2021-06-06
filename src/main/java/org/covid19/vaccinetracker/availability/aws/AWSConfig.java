package org.covid19.vaccinetracker.availability.aws;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "aws")
@Data
public class AWSConfig {
    private String calendarByDistrictLambdaArn;
    private String calendarByPinLambdaArn;
    private String sendTelegramMsgLambdaArn;

    @Bean
    public AWSLambda awsLambda() {
        return AWSLambdaClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(Regions.AP_SOUTH_1).build();
    }

    @Bean
    public AWSLambdaAsync awsLambdaAsync() {
        return AWSLambdaAsyncClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(Regions.AP_SOUTH_1).build();
    }
}
