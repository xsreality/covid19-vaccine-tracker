package com.abhinav.covid19.vaccinetracker.model;

import lombok.Value;

@Value
public class UserRequest {
    private String chatId;
    private String pincode;
}
