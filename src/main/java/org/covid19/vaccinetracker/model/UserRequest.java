package org.covid19.vaccinetracker.model;

import lombok.Value;

@Value
public class UserRequest {
    String chatId;
    String pincode;
}
