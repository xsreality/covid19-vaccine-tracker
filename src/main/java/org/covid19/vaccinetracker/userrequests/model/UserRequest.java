package org.covid19.vaccinetracker.userrequests.model;

import java.util.List;

import lombok.Value;

@Value
public class UserRequest {
    String chatId;
    List<String> pincodes;
    List<Integer> districts;
    String age;
    String dose;
    String vaccine;
    String lastNotifiedAt;
}
