package org.covid19.vaccinetracker.notifications.bot;

import org.covid19.vaccinetracker.model.Center;

import java.util.List;

public interface BotService {
    boolean notifyAvailability(String userId, List<Center> vaccineCenters);
    boolean notify(String userId, String text);

    void notifyOwner(String message);
}
