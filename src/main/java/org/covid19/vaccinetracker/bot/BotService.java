package org.covid19.vaccinetracker.bot;

import org.covid19.vaccinetracker.model.Center;

import java.util.List;

public interface BotService {
    void notify(String userId, List<Center> vaccineCenters);
}
