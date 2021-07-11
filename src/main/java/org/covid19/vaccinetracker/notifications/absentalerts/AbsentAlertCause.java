package org.covid19.vaccinetracker.notifications.absentalerts;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

import static java.util.Objects.isNull;

@Builder
@Data
public class AbsentAlertCause {
    private String userId;
    private String pincode;
    private String lastNotified;
    private List<String> causes;
    private List<String> alternatives;

    public void addCause(String cause) {
        if (isNull(causes)) {
            causes = new ArrayList<>();
        }
        causes.add(cause);
    }

    public void addAlternative(String alternative) {
        if (isNull(alternatives)) {
            alternatives = new ArrayList<>();
        }
        alternatives.add(alternative);
    }
}
