package org.covid19.vaccinetracker.bot;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.persistence.KafkaStateStores;
import org.covid19.vaccinetracker.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.kafka.core.KafkaTemplate;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

import static org.telegram.abilitybots.api.objects.Flag.MESSAGE;
import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@SuppressWarnings("unused")
@Slf4j
public class TelegramBot extends AbilityBot implements BotService, ApplicationContextAware {
    private static Long CHAT_ID;
    private static Long CHANNEL_ID;
    private BotBackend botBackend;
    private KafkaTemplate<String, UserRequest> userRequestKafkaTemplate;
    private KafkaStateStores stateStores;

    public TelegramBot(String botToken, String botUsername, DBContext db, String creatorId, String channelId) {
        super(botToken, botUsername, db);
        CHAT_ID = Long.valueOf(creatorId);
        CHANNEL_ID = Long.valueOf(channelId);
    }

    @Override
    public long creatorId() {
        return CHAT_ID.intValue();
    }

    public Ability start() {
        return Ability
                .builder()
                .name("start")
                .info("Subscribe to Covid19 Vaccine Tracker")
                .locality(ALL)
                .privacy(PUBLIC)
                .input(0)
                .action(ctx -> {
                    String message = "Welcome to COVID19 Vaccine Tracker!\n\n" +
                            "To automatically receive notification when Vaccine becomes available near you, send your pin code.\n\n" +
                            "Stay safe and keep social distancing!";
                    silent.send(message, ctx.chatId());
                })
                .build();
    }

    public Ability catchAll() {
        return Ability
                .builder()
                .name(DEFAULT)
                .flag(MESSAGE)
                .privacy(PUBLIC).locality(ALL)
                .input(0)
                .action(ctx -> {
                    if (ctx.update().hasMessage() && ctx.update().getMessage().hasText()) {
                        String pincodes = ctx.update().getMessage().getText();
                        if (!Utils.allValidPincodes(pincodes)) {
                            String msg = "Send valid pin code to receive notification when vaccine becomes available in your area.";
                            silent.send(msg, ctx.chatId());
                            return;
                        }
                        List<String> pincodesAsList = Utils.splitPincodes(pincodes);
                        if (pincodesAsList.size() > 3) {
                            String msg = "Maximum 3 pin codes can be notified.";
                            silent.send(msg, ctx.chatId());
                            return;
                        }
                        String chatId = getChatId(ctx.update());
                        this.botBackend.acceptUserRequest(chatId, pincodesAsList);
                        silent.send("Okay! I will notify you when vaccine is available in centers near your location.\n\n" +
                                "You can set multiple pin codes by sending them together separated by comma (,). Maximum 3 pin codes are allowed.", ctx.chatId());

                        // send an update to Bot channel
                        String channelMsg = String.format("User %s (%s) set notification preference for pin code(s) %s",
                                Utils.translateName(ctx.update().getMessage().getChat()), chatId, pincodes);
                        silent.send(channelMsg, CHANNEL_ID);
                    }
                })
                .build();
    }

    private String getChatId(Update update) {
        return update.hasMessage() ?
                String.valueOf(update.getMessage().getChatId()) :
                String.valueOf(update.getCallbackQuery().getMessage().getChatId());
    }

    @Override
    public boolean notify(String userId, List<Center> eligibleCenters) {
        String text = Utils.buildNotificationMessage(eligibleCenters);
        SendMessage telegramMessage = SendMessage.builder()
                .chatId(userId)
                .text(text)
                .build();
        log.info("Sending notification to {} with text \"{}\"", userId, text);
        try {
            this.execute(telegramMessage);
            return true;
        } catch (TelegramApiException e) {
            log.error("Error sending telegram message to user id {}, error message: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public void summary(AtomicInteger processedPincodes, AtomicInteger failedCalls, AtomicInteger notificationsSent) {
        log.info("Sending summary notification");
        String text = String.format("Processed pin codes: %d, Failed Cowin API calls: %d, Notifications sent: %d", processedPincodes.get(), failedCalls.get(), notificationsSent.get());
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(CHANNEL_ID))
                .text(text)
                .build();
        try {
            this.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending summary notification error message: {}", e.getMessage());
        }
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.stateStores = (KafkaStateStores) applicationContext.getBean("kafkaStateStores");
        this.botBackend = (BotBackend) applicationContext.getBean("botBackend");
    }
}
