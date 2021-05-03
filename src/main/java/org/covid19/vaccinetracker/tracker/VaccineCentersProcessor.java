package org.covid19.vaccinetracker.tracker;

import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;

@Component
public class VaccineCentersProcessor {
    public boolean areVaccineCentersAvailable(VaccineCenters vaccineCenters) {
        return nonNull(vaccineCenters.centers) && !vaccineCenters.centers.isEmpty();
    }

    public boolean areVaccineCentersAvailableFor18plus(VaccineCenters vaccineCenters) {
        return nonNull(vaccineCenters.centers) && vaccineCenters.centers
                .stream()
                .anyMatch(center -> center.sessions
                        .stream()
                        .anyMatch(session -> has18plus(session) && hasCapacity(session)));
    }

    private boolean hasCapacity(Session session) {
        return session.availableCapacity > 0;
    }

    private boolean has18plus(Session session) {
        return session.getMinAgeLimit() >= 18;
    }
}
