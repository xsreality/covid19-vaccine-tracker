package org.covid19.vaccinetracker.model;

import java.util.Set;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class UsersByPincode {
    private final String pincode;
    private final Set<String> users;

    public UsersByPincode merge(String userId) {
        this.users.add(userId);
        return this;
    }
}
