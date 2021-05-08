package org.covid19.vaccinetracker.bot;

import org.covid19.vaccinetracker.model.Center;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface BotService {
    boolean notify(String userId, List<Center> vaccineCenters);

    void notifyOwner(String message);
}
