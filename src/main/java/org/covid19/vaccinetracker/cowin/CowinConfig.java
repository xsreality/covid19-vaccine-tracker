package org.covid19.vaccinetracker.cowin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties("cowin")
@Data
public class CowinConfig {
    private String apiUrl;
}
