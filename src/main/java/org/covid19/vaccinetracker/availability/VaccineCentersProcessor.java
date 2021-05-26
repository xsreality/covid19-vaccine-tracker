package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Component
public class VaccineCentersProcessor {
    public boolean areVaccineCentersAvailable(VaccineCenters vaccineCenters) {
        return nonNull(vaccineCenters) && nonNull(vaccineCenters.centers) && !vaccineCenters.centers.isEmpty();
    }

    public boolean areVaccineCentersAvailableFor18plus(VaccineCenters vaccineCenters) {
        return nonNull(vaccineCenters.centers) && vaccineCenters.centers
                .stream()
                .anyMatch(center -> center.sessions
                        .stream()
                        .anyMatch(session -> session.ageLimit18AndAbove() && session.hasCapacity()));
    }

    public boolean hasCapacity(Session session) {
        return (session.availableCapacityDose1 > 1)
                && (session.availableCapacity == (session.availableCapacityDose1 + session.availableCapacityDose2));
    }

    public List<Center> eligibleVaccineCenters(VaccineCenters vaccineCenters, boolean shouldAlertAbove45) {
        List<Center> eligibleCenters = new ArrayList<>();

        if (isNull(vaccineCenters.centers)) {
            return eligibleCenters;
        }

        vaccineCenters.centers.forEach(center -> {
            List<Session> eligibleSessions = new ArrayList<>();
            center.sessions.forEach(session -> {
                if (shouldAlertAbove45) { // some users should be alerted for 45 too
                    if (session.ageLimit18AndAbove() && session.hasCapacity()) {
                        eligibleSessions.add(session);
                    }
                } else {
                    if (session.ageLimitExactly18() && session.hasCapacity()) {
                        eligibleSessions.add(session);
                    }
                }
            });
            if (!eligibleSessions.isEmpty()) {
                Center eligibleCenter = buildCenter(center);
                eligibleCenter.setSessions(eligibleSessions);
                eligibleCenters.add(eligibleCenter);
            }
        });
        return eligibleCenters;
    }

    private Center buildCenter(Center center) {
        return Center.builder()
                .centerId(center.getCenterId())
                .name(center.getName())
                .stateName(center.getStateName())
                .districtName(center.getDistrictName())
                .blockName(center.getBlockName())
                .pincode(center.getPincode())
                .feeType(center.getFeeType())
                .from(center.getFrom())
                .to(center.getTo())
                .latitude(center.getLatitude())
                .longitude(center.getLongitude())
                .build();
    }
}
