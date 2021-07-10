package org.covid19.vaccinetracker.notifications.absentalerts;

import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotification;

import lombok.Builder;
import lombok.Data;

/**
 * This class is used as a source of information for generating the cause(s) of absent alerts for
 * the user.
 */
@Builder
@Data
public class AbsentAlertSource {
    private String userId;
    private String pincode;
    private String age;
    private String dose;
    private String vaccine;
    private UserNotification latestNotification; // can be null
}
