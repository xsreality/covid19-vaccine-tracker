package org.covid19.vaccinetracker.model;

import java.util.List;

import lombok.Value;

@Value
public class UserRequest {
    String chatId;
    List<String> pincodes;
    String lastNotifiedAt;
}
