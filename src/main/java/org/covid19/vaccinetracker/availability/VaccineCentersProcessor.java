package org.covid19.vaccinetracker.availability;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.userrequests.model.Age;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Component
public class VaccineCentersProcessor {
    private final List<String> usersOver45;

    private final VaccinePersistence vaccinePersistence;
    private final UserRequestManager userRequestManager;

    public VaccineCentersProcessor(VaccinePersistence vaccinePersistence,
                                   UserRequestManager userRequestManager,
                                   @Value("${users.over45}") List<String> usersOver45) {
        this.vaccinePersistence = vaccinePersistence;
        this.usersOver45 = usersOver45;
        this.userRequestManager = userRequestManager;
    }

    public void persistVaccineCenters(VaccineCenters vaccineCenters) {
        vaccinePersistence.persistVaccineCenters(vaccineCenters);
    }

    public boolean hasCapacity(Session session) {
        return (session.availableCapacityDose1 > 1)
                && (session.availableCapacity == (session.availableCapacityDose1 + session.availableCapacityDose2));
    }

    public List<Center> eligibleVaccineCenters(VaccineCenters vaccineCenters, String user) {
        List<Center> eligibleCenters = new ArrayList<>();

        if (isNull(vaccineCenters.centers)) {
            return eligibleCenters;
        }

        vaccineCenters.centers.forEach(center -> {
            List<Session> eligibleSessions = center.getSessions().stream()
                    .filter(Session::hasCapacity)
                    .filter(session -> specialUser(session, user) || eligibleCenterForUser(session, user))
                    .collect(Collectors.toList());

            if (!eligibleSessions.isEmpty()) {
                Center eligibleCenter = buildCenter(center);
                eligibleCenter.setSessions(eligibleSessions);
                eligibleCenters.add(eligibleCenter);
            }
        });
        return eligibleCenters;
    }

    private boolean eligibleCenterForUser(Session session, String user) {
        final Age userAgePreference = userRequestManager.getUserAgePreference(user);
        if (sessionAndUserValidFor18(session, userAgePreference)
                || sessionAndUserValidFor45(session, userAgePreference)) {
            return true;
        }
        if (Age.AGE_BOTH.equals(userAgePreference)) {
            return true;
        }
        return false;
    }

    private boolean sessionAndUserValidFor45(Session session, Age userAgePreference) {
        return Age.AGE_45.equals(userAgePreference) && session.ageLimitExactly45();
    }

    private boolean sessionAndUserValidFor18(Session session, Age userAgePreference) {
        return Age.AGE_18_44.equals(userAgePreference) && session.ageLimitExactly18();
    }

    private boolean specialUser(Session session, String user) {
        return usersOver45.contains(user) && session.ageLimit18AndAbove();
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
