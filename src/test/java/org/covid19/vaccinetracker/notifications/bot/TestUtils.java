package org.covid19.vaccinetracker.notifications.bot;

import org.jetbrains.annotations.NotNull;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestUtils {
    public static final User USER = new User(1L, "Abhinav", false, "last", "username", null, false, false, false);
    public static final User CREATOR = new User(1337L, "creatorFirst", false, "creatorLast", "creatorUsername", null, false, false, false);

    private TestUtils() {
    }

    @NotNull
    static Update mockFullUpdate(AbilityBot bot, User user, String args) {
        bot.users().put(USER.getId(), USER);
        bot.users().put(CREATOR.getId(), CREATOR);
        bot.userIds().put(CREATOR.getUserName(), CREATOR.getId());
        bot.userIds().put(USER.getUserName(), USER.getId());

        bot.admins().add(CREATOR.getId());

        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        Message message = mock(Message.class);
        when(message.getText()).thenReturn(args);
        when(message.hasText()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
