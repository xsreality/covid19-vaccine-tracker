package org.covid19.vaccinetracker.notifications.bot;

import com.google.common.annotations.VisibleForTesting;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.persistence.mariadb.repository.StateRepository;
import org.covid19.vaccinetracker.userrequests.model.State;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.covid19.vaccinetracker.userrequests.model.Vaccine;
import org.covid19.vaccinetracker.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.kafka.core.KafkaTemplate;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_18_44;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_45;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_BOTH;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_1;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_2;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_BOTH;
import static org.covid19.vaccinetracker.userrequests.model.Vaccine.COVAXIN;
import static org.covid19.vaccinetracker.userrequests.model.Vaccine.COVISHIELD;
import static org.covid19.vaccinetracker.userrequests.model.Vaccine.SPUTNIK_V;
import static org.telegram.abilitybots.api.objects.Flag.MESSAGE;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@SuppressWarnings("unused")
@Slf4j
public class TelegramBot extends AbilityBot implements BotService, ApplicationContextAware {
    private static Long CHAT_ID;
    private static Long CHANNEL_ID;
    private BotBackend botBackend;
    private KafkaTemplate<String, UserRequest> userRequestKafkaTemplate;
    private StateRepository stateRepository;

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
        return Ability.builder().name("start").info("Subscribe to Covid19 Vaccine Tracker")
                .locality(Locality.ALL).privacy(PUBLIC).input(0).action(ctx -> {
                    String message = String.format("Hi %s! Welcome to COVID19 Vaccine Tracker! कोविड-19 वैक्सीन ट्रैकर में आपका स्वागत है!\n\n" +
                            "To automatically receive notification when Vaccine becomes available near you, send your pin code.\n" +
                            "जब आपके पास वैक्सीन उपलब्ध हो जाता है तो अपने आप ही सूचना प्राप्त करने के लिए अपना पिन कोड भेजें।\n\n" +
                            "Stay safe, wear masks and keep social distancing!\n" +
                            "सुरक्षित रहें, मास्क पहनें और सामाजिक दूरी बनाए रखें!\n\n" +
                            "Note: I will not send alerts from midnight to 6AM in morning so that you can sleep peacefully :-)\n" +
                            "मैं आधी रात से सुबह 6 बजे तक अलर्ट नहीं भेजूंगा ताकि आप चैन से सो सकें :-)", getFirstName(ctx.update()));
                    silent.send(message, ctx.chatId());
                }).build();
    }

    public Ability stop() {
        return Ability.builder().name("stop").info("Stop getting alerts")
                .locality(Locality.ALL).privacy(PUBLIC).input(0).action(ctx -> {
                    String chatId = getChatId(ctx.update());
                    this.botBackend.cancelUserRequest(chatId);
                    String message = String.format("Okay %s, I will no longer send you any alerts. ठीक है, मैं अब आपको कोई अलर्ट नहीं भेजूंगी।\n\n" +
                            "I hope you were able to book vaccine slot with my help. Please send feedback to @xsreality\n" +
                            "मुझे आशा है कि आप मेरी मदद से वैक्सीन स्लॉट बुक करने में सक्षम थे। कृपया प्रतिक्रिया भेजें @xsreality", getFirstName(ctx.update()));
                    silent.execute(SendMessage.builder().chatId(chatId).text(message).parseMode(ParseMode.HTML).build());
                    notifyOwner(String.format("%s (%s, %s) stopped subscription.",
                            Utils.translateName(ctx.update().getMessage().getChat()), chatId, getUserName(ctx)));
                }).build();
    }

    public Ability about() {
        return Ability.builder().name("about").info("About CoWIN Alerts bot")
                .locality(Locality.ALL).privacy(PUBLIC).input(0).action(ctx -> {
                    String chatId = getChatId(ctx.update());
                    String message = "Hi! My name is Abhinav Sonkar (@xsreality). I am a software engineer.\n\n" +
                            "I developed this bot to help people book CoWIN slots and get vaccinated.\n\n" +
                            "With over 7000 registered users, 500 districts and 1000's of pincodes, it is becoming expensive to " +
                            "keep running the bot. If you like my work and want to support it, <a href=\"https://www.buymeacoffee.com/xsreality\">buy me a coffee</a>.";
                    silent.execute(SendMessage.builder().chatId(chatId).text(message).parseMode(ParseMode.HTML).build());
                    notifyOwner(String.format("%s (%s, %s) checked About page.",
                            Utils.translateName(ctx.update().getMessage().getChat()), chatId, getUserName(ctx)));
                }).build();
    }

    public Ability subscriptions() {
        return Ability.builder().name("subscriptions").info("Show my subscriptions")
                .locality(Locality.ALL).privacy(PUBLIC).input(0).action(ctx -> {
                    String chatId = String.valueOf(ctx.chatId());
                    final UserRequest user = this.botBackend.fetchUserSubscriptions(chatId);
                    String message;
                    if (isNull(user) || isNull(user.getPincodes()) || user.getPincodes().isEmpty()) {
                        message = "You have no pincodes subscribed. Just send pincodes separated by comma (,) to subscribe.";
                    } else {
                        message = String.format("You are currently subscribed to pincodes: %s\n\n" +
                                        "Your age preference: %s\n\n" +
                                        "Your dose preference: %s\n\n" +
                                        "Your vaccine preference: %s",
                                Utils.joinPincodes(user.getPincodes()),
                                isNull(user.getAge()) ? "18-44" : user.getAge(),
                                isNull(user.getDose()) ? "Dose 1" : user.getDose(),
                                isNull(user.getVaccine()) ? "ALL" : user.getVaccine());
                    }
                    silent.send(message, ctx.chatId());
                    notifyOwner(String.format("%s (%s, %s) viewed existing subscriptions.",
                            Utils.translateName(ctx.update().getMessage().getChat()), chatId, getUserName(ctx)));
                }).build();
    }

    public Ability catchAll() {
        return Ability
                .builder()
                .name(DEFAULT)
                .flag(MESSAGE)
                .privacy(PUBLIC).locality(Locality.ALL)
                .input(0)
                .action(ctx -> {
                    if (ctx.update().hasMessage() && ctx.update().getMessage().hasText()) {
                        String pincodes = ctx.update().getMessage().getText();
                        if (invalidPincodes(ctx, pincodes)) {
                            return;
                        }

                        List<String> pincodesAsList = Utils.splitPincodes(pincodes);
                        if (tooManyPincodes(ctx, pincodesAsList)) {
                            return;
                        }

                        String chatId = getChatId(ctx.update());
                        String firstName = getFirstName(ctx.update());

                        this.botBackend.acceptUserRequest(chatId, pincodesAsList);
                        State state = this.stateRepository.findByPincode(pincodesAsList.get(0));

                        String localizedAckMessage = Utils.localizedAckText(state);
                        silent.send(String.format("Okay %s! I will notify you when vaccine is available in centers near your location.\n" +
                                "You can set multiple pincodes by sending them together separated by comma (,). Maximum 5 pincodes are allowed.\n" +
                                "Make sure notification is turned on for this bot so you don't miss any alerts!\n\n" +
                                "Send /age to set your age preference.\n\n" +
                                "Send /dose to set your dose preference.\n\n" +
                                "Send /vaccine to set your vaccine preference.\n\n" +
                                "Send /subscriptions to view your current subscription.\n\n" +
                                "Send /about to see more information about this bot.\n\n" +
                                localizedAckMessage, firstName), ctx.chatId());

                        // send an update to Bot channel
                        notifyOwner(String.format("%s (%s, %s) set notification preference for pincode(s) %s",
                                Utils.translateName(ctx.update().getMessage().getChat()), chatId, getUserName(ctx), pincodes));
                    }
                })
                .build();
    }

    public ReplyFlow ageSelectionFlow() {
        Reply age18Flow = Reply.of((bot, upd) -> {
            removeKeyboard(upd);
            botBackend.updateAgePreference(getChatId(upd), AGE_18_44);
            silent.execute(SendMessage.builder().chatId(getChatId(upd)).text("I have updated your age preference to 18-44").build());
            notifyOwner(String.format("%s (%s) set age preference to 18-44",
                    Utils.translateName(upd.getCallbackQuery().getMessage().getChat()), getChatId(upd)));
        }, hasMessage("18-44"));

        Reply age45Flow = Reply.of((bot, upd) -> {
            removeKeyboard(upd);
            botBackend.updateAgePreference(getChatId(upd), AGE_45);
            silent.execute(SendMessage.builder().chatId(getChatId(upd)).text("I have updated your age preference to 45+").build());
            notifyOwner(String.format("%s (%s) set age preference to 45+",
                    Utils.translateName(upd.getCallbackQuery().getMessage().getChat()), getChatId(upd)));
        }, hasMessage("45+"));

        Reply ageBothFlow = Reply.of((bot, upd) -> {
            removeKeyboard(upd);
            botBackend.updateAgePreference(getChatId(upd), AGE_BOTH);
            silent.execute(SendMessage.builder().chatId(getChatId(upd)).text("I have updated your age preference to both 18-44 and 45+").build());
            notifyOwner(String.format("%s (%s) set age preference to both 18-44 and 45+",
                    Utils.translateName(upd.getCallbackQuery().getMessage().getChat()), getChatId(upd)));
        }, hasMessage("both"));

        return ReplyFlow.builder(db, 110)
                .action((bot, update) -> silent.execute(BotUtils.buildAgeSelectionKeyboard(getChatId(update))))
                .onlyIf(isCallbackOrMessage("/age"))
                .next(age18Flow)
                .next(age45Flow)
                .next(ageBothFlow)
                .build();
    }

    public ReplyFlow doseSelectionFlow() {
        Reply dose1Flow = Reply.of((bot, upd) -> {
            removeKeyboard(upd);
            botBackend.updateDosePreference(getChatId(upd), DOSE_1);
            silent.execute(SendMessage.builder().chatId(getChatId(upd)).text("I have updated your dose preference to 'First Dose'").build());
            notifyOwner(String.format("%s (%s) set dose preference to Dose1",
                    Utils.translateName(upd.getCallbackQuery().getMessage().getChat()), getChatId(upd)));
        }, hasMessage("dose1"));

        Reply dose2Flow = Reply.of((bot, upd) -> {
            removeKeyboard(upd);
            botBackend.updateDosePreference(getChatId(upd), DOSE_2);
            silent.execute(SendMessage.builder().chatId(getChatId(upd)).text("I have updated your dose preference to 'Second Dose'").build());
            notifyOwner(String.format("%s (%s) set dose preference to Dose2",
                    Utils.translateName(upd.getCallbackQuery().getMessage().getChat()), getChatId(upd)));
        }, hasMessage("dose2"));

        Reply doseBothFlow = Reply.of((bot, upd) -> {
            removeKeyboard(upd);
            botBackend.updateDosePreference(getChatId(upd), DOSE_BOTH);
            silent.execute(SendMessage.builder().chatId(getChatId(upd)).text("I have updated your dose preference to both First and Second Dose").build());
            notifyOwner(String.format("%s (%s) set dose preference to both Dose 1 and 2",
                    Utils.translateName(upd.getCallbackQuery().getMessage().getChat()), getChatId(upd)));
        }, hasMessage("both"));

        return ReplyFlow.builder(db, 115)
                .action((bot, update) -> silent.execute(BotUtils.buildDoseSelectionKeyboard(getChatId(update))))
                .onlyIf(isCallbackOrMessage("/dose"))
                .next(dose1Flow)
                .next(dose2Flow)
                .next(doseBothFlow)
                .build();
    }

    public ReplyFlow vaccineSelectionFlow() {
        Reply covishieldFlow = Reply.of((bot, upd) -> {
            removeKeyboard(upd);
            botBackend.updateVaccinePreference(getChatId(upd), COVISHIELD);
            silent.execute(SendMessage.builder().chatId(getChatId(upd)).text("I have updated your vaccine preference to COVISHIELD").build());
            notifyOwner(String.format("%s (%s) set vaccine preference to COVISHIELD",
                    Utils.translateName(upd.getCallbackQuery().getMessage().getChat()), getChatId(upd)));
        }, hasMessage("covishield"));

        Reply covaxinFlow = Reply.of((bot, upd) -> {
            removeKeyboard(upd);
            botBackend.updateVaccinePreference(getChatId(upd), COVAXIN);
            silent.execute(SendMessage.builder().chatId(getChatId(upd)).text("I have updated your vaccine preference to COVAXIN").build());
            notifyOwner(String.format("%s (%s) set vaccine preference to COVAXIN",
                    Utils.translateName(upd.getCallbackQuery().getMessage().getChat()), getChatId(upd)));
        }, hasMessage("covaxin"));

        Reply sputnikvFlow = Reply.of((bot, upd) -> {
            removeKeyboard(upd);
            botBackend.updateVaccinePreference(getChatId(upd), SPUTNIK_V);
            silent.execute(SendMessage.builder().chatId(getChatId(upd)).text("I have updated your vaccine preference to SPUTNIK V").build());
            notifyOwner(String.format("%s (%s) set vaccine preference to SPUTNIK V",
                    Utils.translateName(upd.getCallbackQuery().getMessage().getChat()), getChatId(upd)));
        }, hasMessage("sputnikv"));

        Reply anyVaccineFlow = Reply.of((bot, upd) -> {
            removeKeyboard(upd);
            botBackend.updateVaccinePreference(getChatId(upd), Vaccine.ALL);
            silent.execute(SendMessage.builder().chatId(getChatId(upd)).text("I have updated your vaccine preference to ALL vaccines").build());
            notifyOwner(String.format("%s (%s) set vaccine preference to ALL",
                    Utils.translateName(upd.getCallbackQuery().getMessage().getChat()), getChatId(upd)));
        }, hasMessage("all"));

        return ReplyFlow.builder(db, 120)
                .action((bot, update) -> silent.execute(BotUtils.buildVaccineSelectionKeyboard(getChatId(update))))
                .onlyIf(isCallbackOrMessage("/vaccine"))
                .next(covishieldFlow)
                .next(covaxinFlow)
                .next(sputnikvFlow)
                .next(anyVaccineFlow)
                .build();
    }

    private boolean tooManyPincodes(MessageContext ctx, List<String> pincodesAsList) {
        if (pincodesAsList.size() > 5) {
            String msg = "Maximum 5 pincodes can be notified.\n\n" +
                    "अधिकतम 5 पिन कोड अधिसूचित किए जा सकते हैं।";
            silent.send(msg, ctx.chatId());
            return true;
        }
        return false;
    }

    private boolean invalidPincodes(MessageContext ctx, String pincodes) {
        if (!Utils.allValidPincodes(pincodes)) {
            String msg = "Send valid pincode to receive notification when vaccine becomes available in your area.\n" +
                    "जब आपके क्षेत्र में वैक्सीन उपलब्ध हो जाए तो अधिसूचना प्राप्त करने के लिए पिन कोड भेजें।\n\n" +
                    "/age - To set your age preference. अपनी आयु वरीयता निर्धारित करें।\n\n" +
                    "/dose - To set your dose preference. अपनी खुराक वरीयता निर्धारित करें।\n\n" +
                    "/vaccine - To set your vaccine preference. अपनी वैक्सीन वरीयता निर्धारित करें।\n\n" +
                    "/subscriptions - To see your current subscriptions. अपने वर्तमान पिनकोड देखने के लिए.\n\n" +
                    "/stop - To stop receiving alerts. अलर्ट प्राप्त करना बंद करने के लिए.\n\n" +
                    "Note: I will not send alerts from midnight to 6AM in morning so that you can sleep peacefully :-)\n" +
                    "मैं आधी रात से सुबह 6 बजे तक अलर्ट नहीं भेजूंगा ताकि आप चैन से सो सकें :-)";
            silent.send(msg, ctx.chatId());
            return true;
        }
        return false;
    }

    private void removeKeyboard(Update upd) {
        DeleteMessage msg = new DeleteMessage();
        msg.setChatId(getChatId(upd));
        msg.setMessageId(upd.getCallbackQuery().getMessage().getMessageId());
        silent.execute(msg);
    }

    @NotNull
    private Predicate<Update> hasMessageWith(String msg) {
        return upd -> Flag.MESSAGE.test(upd) && upd.getMessage().getText().equalsIgnoreCase(msg);
    }


    private Predicate<Update> hasMessage(String desired) {
        return upd -> {
            if (upd.hasCallbackQuery()) {
                String actual = upd.getCallbackQuery().getData();
                return actual.equalsIgnoreCase(desired);
            }
            return false;
        };
    }

    private Predicate<Update> isCallbackOrMessage(String msg) {
        return upd -> (upd.hasMessage() && upd.getMessage().hasText() && upd.getMessage().getText().equalsIgnoreCase(msg)) ||
                (upd.hasCallbackQuery() && upd.getCallbackQuery().getData().equalsIgnoreCase(msg));
    }

    private String getUserName(MessageContext ctx) {
        return nonNull(ctx.user().getUserName()) ? ctx.user().getUserName() : "";
    }

    private String getChatId(Update update) {
        return update.hasMessage() ?
                String.valueOf(update.getMessage().getChatId()) :
                String.valueOf(update.getCallbackQuery().getMessage().getChatId());
    }

    private String getFirstName(Update update) {
        return update.hasMessage() ?
                String.valueOf(update.getMessage().getChat().getFirstName()) :
                String.valueOf(update.getCallbackQuery().getMessage().getChat().getFirstName());
    }

    @Override
    public boolean notifyAvailability(String userId, List<Center> eligibleCenters) {
        String text = Utils.buildNotificationMessage(eligibleCenters);
        SendMessage telegramMessage = SendMessage.builder()
                .chatId(userId)
                .text(text)
                .parseMode(ParseMode.HTML)
                .build();
        log.info("Sending notification to {} for pincode {}", userId, eligibleCenters.get(0).getPincode());
        try {
            this.execute(telegramMessage);
            return true;
        } catch (TelegramApiException e) {
            log.error("Error sending telegram message to user id {}, error message: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public void notifyOwner(String text) {
        SendMessage telegramMessage = SendMessage.builder()
                .chatId(String.valueOf(CHANNEL_ID))
                .text(text)
                .parseMode(ParseMode.HTML)
                .build();
        silent.execute(telegramMessage);
    }

    @Override
    public boolean notify(String chatId, String text) {
        SendMessage telegramMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode(ParseMode.HTML)
                .build();
        try {
            this.execute(telegramMessage);
            return true;
        } catch (TelegramApiException e) {
            log.error("Error sending telegram message to user id {}, error message: {}", chatId, e.getMessage());
            return false;
        }
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.botBackend = (BotBackend) applicationContext.getBean("botBackend");
        this.stateRepository = (StateRepository) applicationContext.getBean("stateRepository");
        TelegramConfig telegramConfig = (TelegramConfig) applicationContext.getBean("telegramConfig");
        if (telegramConfig.isEnabled()) {
            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(this);
            } catch (TelegramApiException e) {
                throw new IllegalStateException("Unable to register Telegram bot", e);
            }
        }
    }

    @VisibleForTesting
    void setSilent(SilentSender sender) {
        this.silent = sender;
    }
}
