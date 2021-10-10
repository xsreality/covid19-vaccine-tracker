package org.covid19.vaccinetracker.notifications.absentalerts;

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
    private List<String> recents;

    public void addCause(String cause) {
        if (isNull(causes)) {
            causes = new ArrayList<>();
        }
        causes.add(cause);
    }

    public void addRecent(String recent) {
        if (isNull(recents)) {
            recents = new ArrayList<>();
        }
        recents.add(recent);
    }
}
