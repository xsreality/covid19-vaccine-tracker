package org.covid19.vaccinetracker.availability;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "availability")
public class AvailabilityConfig {
    private List<String> priorityDistricts;
}
