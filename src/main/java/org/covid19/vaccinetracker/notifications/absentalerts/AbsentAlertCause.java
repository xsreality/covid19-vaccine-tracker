package org.covid19.vaccinetracker.notifications.absentalerts;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AbsentAlertCause {
    private String userId;
    private String pincode;
    private List<String> causes;
}
