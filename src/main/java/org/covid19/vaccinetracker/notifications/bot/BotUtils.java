package org.covid19.vaccinetracker.notifications.bot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class BotUtils {
    public static SendMessage buildAgeSelectionKeyboard(String chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText("Choose preferred age");
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder().text("18-44").callbackData("18-44").build());
        row.add(InlineKeyboardButton.builder().text("45+").callbackData("45+").build());
        row.add(InlineKeyboardButton.builder().text("Both").callbackData("both").build());
        keyboard.add(row);

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }

    public static SendMessage buildDoseSelectionKeyboard(String chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText("Choose preferred dose");
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder().text("Dose 1").callbackData("dose1").build());
        row.add(InlineKeyboardButton.builder().text("Dose 2").callbackData("dose2").build());
        row.add(InlineKeyboardButton.builder().text("Both").callbackData("both").build());
        keyboard.add(row);

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }

    public static SendMessage buildVaccineSelectionKeyboard(String chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText("Choose preferred vaccine");
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder().text("COVISHIELD").callbackData("covishield").build());
        row.add(InlineKeyboardButton.builder().text("COVAXIN").callbackData("covaxin").build());
        keyboard.add(row);
        row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder().text("SPUTNIK V").callbackData("sputnikv").build());
        row.add(InlineKeyboardButton.builder().text("ALL").callbackData("all").build());
        keyboard.add(row);

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }
}
