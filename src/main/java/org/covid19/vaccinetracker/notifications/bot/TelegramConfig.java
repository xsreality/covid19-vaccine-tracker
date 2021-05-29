package org.covid19.vaccinetracker.notifications.bot;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.abilitybots.api.db.MapDBContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "telegram")
@Data
public class TelegramConfig {
    private boolean enabled;
    private List<String> blackListUsers = new ArrayList<>();
    private String dbPath;
    private String botUsername;
    private String botToken;
    private String creatorId;
    private String chatId;

    @Bean
    public TelegramBot telegramBot() {

        DB db = DBMaker
                .fileDB(new File(dbPath))
                .fileMmapEnableIfSupported()
                .closeOnJvmShutdown()
                .transactionEnable()
                .make();

        return new TelegramBot(botToken, botUsername, new MapDBContext(db), creatorId, chatId);
    }

}
