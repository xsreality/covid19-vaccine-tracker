package org.covid19.vaccinetracker.notifications.bot;

import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.userrequests.model.Age;
import org.covid19.vaccinetracker.userrequests.model.Dose;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.covid19.vaccinetracker.userrequests.model.Vaccine;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static java.util.Collections.emptyList;

@Slf4j
@Service
public class BotBackend {
    private final UserRequestManager userRequestManager;

    public BotBackend(UserRequestManager userRequestManager) {
        this.userRequestManager = userRequestManager;
    }

    public void acceptUserRequest(String userId, List<String> pincodes) {
        userRequestManager.acceptUserRequest(userId, pincodes);
    }

    public void cancelUserRequest(String userId) {
        userRequestManager.acceptUserRequest(userId, emptyList());
    }

    public UserRequest fetchUserSubscriptions(String userId) {
        return userRequestManager.fetchUserRequest(userId);
    }

    public void updateAgePreference(String chatId, Age age) {
        userRequestManager.updateAgePreference(chatId, age);
    }

    public void updateDosePreference(String chatId, Dose dose) {
        userRequestManager.updateDosePreference(chatId, dose);
    }

    public void updateVaccinePreference(String chatId, Vaccine vaccine) {
        userRequestManager.updateVaccinePreference(chatId, vaccine);
    }
}
