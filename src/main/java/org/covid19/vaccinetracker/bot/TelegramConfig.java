package org.covid19.vaccinetracker.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "telegram")
@Data
public class TelegramConfig {
    private boolean enabled;
    private List<String> blackListUsers = new ArrayList<>();
}
