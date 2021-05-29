package org.covid19.vaccinetracker.notifications.bot;

import org.covid19.vaccinetracker.model.Center;

import java.util.List;

public interface BotService {
    boolean notify(String userId, List<Center> vaccineCenters);

    void notifyOwner(String message);
}
