package org.covid19.vaccinetracker.notifications.bot;

import org.covid19.vaccinetracker.persistence.mariadb.repository.StateRepository;
import org.covid19.vaccinetracker.userrequests.model.State;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.covid19.vaccinetracker.userrequests.model.Vaccine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.db.MapDBContext;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.io.IOException;
import java.util.List;

import static org.covid19.vaccinetracker.notifications.bot.TestUtils.USER;
import static org.covid19.vaccinetracker.notifications.bot.TestUtils.mockFullUpdate;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_18_44;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_45;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TelegramBotTest {
    public static final String USER_ID = "1337";
    public static final long CHAT_ID = 1337L;

    private TelegramBot bot;
    private DBContext db;

    @Mock
    private BotBackend botBackend;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private SilentSender silent;

    @BeforeEach
    public void setup() {
        db = MapDBContext.offlineInstance("test");
        bot = new TelegramBot("token", "bot_username", db, USER_ID, USER_ID);

        ReflectionTestUtils.setField(bot, "botBackend", botBackend);
        ReflectionTestUtils.setField(bot, "stateRepository", stateRepository);
        bot.setSilent(silent);
        bot.onRegister();
    }

    @AfterEach
    public void tearDown() throws IOException {
        db.clear();
        db.close();
    }

    @Test
    public void testSubscriptionsWithNoPincodes() {
        Update update = mock(Update.class);
        MessageContext context = MessageContext.newContext(update, new User(), CHAT_ID, bot);
        Chat chat = mock(Chat.class);
        when(chat.getFirstName()).thenReturn(USER.getFirstName());
        Message message = mock(Message.class);
        when(update.getMessage()).thenReturn(message);
        when(update.getMessage().getChat()).thenReturn(chat);

        when(botBackend.fetchUserSubscriptions(USER_ID)).thenReturn(new UserRequest("user_id", List.of(), List.of(), AGE_18_44.toString(), DOSE_1.toString(), Vaccine.ALL.toString(), null));
        bot.subscriptions().action().accept(context);

        verify(silent, times(1)).send("You have no pincodes subscribed. Just send pincodes separated by comma (,) to subscribe.", CHAT_ID);
    }

    @Test
    public void testSubscriptionsWithPincodes() {
        Update update = mock(Update.class);
        MessageContext context = MessageContext.newContext(update, new User(), CHAT_ID, bot);
        Chat chat = mock(Chat.class);
        when(chat.getFirstName()).thenReturn(USER.getFirstName());
        Message message = mock(Message.class);
        when(update.getMessage()).thenReturn(message);
        when(update.getMessage().getChat()).thenReturn(chat);

        when(botBackend.fetchUserSubscriptions(USER_ID)).thenReturn(new UserRequest("user_id", List.of("110022", "122001"), List.of(), AGE_45.toString(), DOSE_1.toString(), Vaccine.ALL.toString(), null));
        bot.subscriptions().action().accept(context);

        verify(silent, times(1)).send("You are currently subscribed to pincodes: 110022,122001\n\n" +
                "Your age preference: 45+\n\n" +
                "Your dose preference: Dose 1\n\n" +
                "Your vaccine preference: All", CHAT_ID);
    }

    @Test
    public void testCatchAll_WithInvalidPincode() {
        Update update = mockFullUpdate(bot, USER, "Random message");
        MessageContext context = MessageContext.newContext(update, USER, CHAT_ID, bot);

        bot.catchAll().action().accept(context);

        verify(silent, times(1)).send("Send valid pincode to receive notification when vaccine becomes available in your area.\n" +
                "जब आपके क्षेत्र में वैक्सीन उपलब्ध हो जाए तो अधिसूचना प्राप्त करने के लिए पिन कोड भेजें।\n\n" +
                "/age - To set your age preference. अपनी आयु वरीयता निर्धारित करें।\n\n" +
                "/dose - To set your dose preference. अपनी खुराक वरीयता निर्धारित करें।\n\n" +
                "/vaccine - To set your vaccine preference. अपनी वैक्सीन वरीयता निर्धारित करें।\n\n" +
                "/subscriptions - To see your current subscriptions. अपने वर्तमान पिनकोड देखने के लिए.\n\n" +
                "/stop - To stop receiving alerts. अलर्ट प्राप्त करना बंद करने के लिए.\n\n" +
                "Note: I will not send alerts from midnight to 6AM in morning so that you can sleep peacefully :-)\n" +
                "मैं आधी रात से सुबह 6 बजे तक अलर्ट नहीं भेजूंगा ताकि आप चैन से सो सकें :-)", CHAT_ID);
    }

    @Test
    public void testCatchAll_WithMoreThanFivePincodes() {
        Update update = mockFullUpdate(bot, USER, "110092, 110093, 110094, 110095, 110096, 110097");
        MessageContext context = MessageContext.newContext(update, USER, CHAT_ID, bot);

        bot.catchAll().action().accept(context);

        verify(silent, times(1)).send("Maximum 5 pincodes can be notified.\n\n" +
                "अधिकतम 5 पिन कोड अधिसूचित किए जा सकते हैं।", CHAT_ID);
    }

    @Test
    public void testCatchAll_WithValidPincode() {
        Update update = mockFullUpdate(bot, USER, "110092");
        Chat chat = mock(Chat.class);
        when(chat.getFirstName()).thenReturn(USER.getFirstName());
        when(update.getMessage().getChat()).thenReturn(chat);

        MessageContext context = MessageContext.newContext(update, USER, CHAT_ID, bot);

        when(stateRepository.findByPincode("110092")).thenReturn(new State(1, "Delhi"));
        bot.catchAll().action().accept(context);

        verify(silent, times(1)).send("Okay Abhinav! I will notify you when vaccine is available in centers near your location.\n" +
                "You can set multiple pincodes by sending them together separated by comma (,). Maximum 5 pincodes are allowed.\n" +
                "Make sure notification is turned on for this bot so you don't miss any alerts!\n\n" +
                "Send /age to set your age preference.\n\n" +
                "Send /dose to set your dose preference.\n\n" +
                "Send /vaccine to set your vaccine preference.\n\n" +
                "Send /subscriptions to view your current subscription.\n\n" +
                "Send /about to see more information about this bot.\n\n" +
                "ठीक है! जब आपके स्थान के पास के केंद्रों में टीका उपलब्ध होगा तो मैं आपको सूचित करूँगा।\n" +
                "आप कई पिन कोड कॉमा (,) द्वारा अलग-अलग सेट कर सकते हैं। अधिकतम 3 पिन कोड की अनुमति है।\n" +
                "सुनिश्चित करें कि अधिसूचना इस बॉट के लिए चालू है ताकि आप किसी भी अलर्ट को न भूलें!\n\n" +
                "अपनी आयु वरीयता निर्धारित करने के लिए /age भेजें।\n\n" +
                "अपनी वर्तमान सदस्यता देखने के लिए /subscriptions भेजें।", CHAT_ID);

        // notify bot owner
        verify(silent, times(1)).execute(any());
    }
}
