package org.covid19.vaccinetracker.availability;

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
                        .anyMatch(session -> ageLimit18AndAbove(session) && hasCapacity(session)));
    }

    public boolean hasCapacity(Session session) {
        return session.availableCapacity > 0;
    }

    public boolean ageLimitExactly18(Session session) {
        return session.getMinAgeLimit() == 18;
    }

    public boolean ageLimit18AndAbove(Session session) {
        return session.getMinAgeLimit() >= 18;
    }
}
