package org.covid19.vaccinetracker.notifications;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.userrequests.model.Age;
import org.covid19.vaccinetracker.userrequests.model.Dose;
import org.covid19.vaccinetracker.userrequests.model.Vaccine;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.covid19.vaccinetracker.userrequests.model.Age.AGE_BOTH;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_BOTH;

@Component
public class VaccineCentersProcessor {
    private final UserRequestManager userRequestManager;

    public VaccineCentersProcessor(UserRequestManager userRequestManager) {
        this.userRequestManager = userRequestManager;
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
                    .filter(session -> eligibleCenterForUser(session, user))
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
        return Stream.of(user)
                .filter(checkAgePreference(session))
                .filter(checkDosePreference(session))
                .filter(checkVaccinePreference(session))
                .map(u -> true)
                .findFirst()
                .orElse(false);
    }

    @NotNull
    private Predicate<String> checkVaccinePreference(Session session) {
        return u -> {
            final Vaccine preference = userRequestManager.getUserVaccinePreference(u);
            return sessionAndUserValidForCovishield(session, preference)
                    || sessionAndUserValidForCovaxin(session, preference)
                    || sessionAndUserValidForSputnikV(session, preference)
                    || Vaccine.ALL.equals(preference);
        };
    }

    @NotNull
    private Predicate<String> checkDosePreference(Session session) {
        return u -> {
            final Dose preference = userRequestManager.getUserDosePreference(u);
            return sessionAndUserValidForDose1(session, preference)
                    || sessionAndUserValidForDose2(session, preference)
                    || DOSE_BOTH.equals(preference);
        };
    }

    @NotNull
    private Predicate<String> checkAgePreference(Session session) {
        return u -> {
            final Age preference = userRequestManager.getUserAgePreference(u);
            return sessionAndUserValidFor18(session, preference)
                    || sessionAndUserValidFor45(session, preference)
                    || AGE_BOTH.equals(preference);
        };
    }

    private boolean sessionAndUserValidFor45(Session session, Age userAgePreference) {
        return Age.AGE_45.equals(userAgePreference) && (session.validForAllAges() || session.validFor45Above());
    }

    private boolean sessionAndUserValidFor18(Session session, Age userAgePreference) {
        return Age.AGE_18_44.equals(userAgePreference) && (session.validForAllAges() || session.validBetween18And44());
    }

    private boolean sessionAndUserValidForDose1(Session session, Dose userDosePreference) {
        return Dose.DOSE_1.equals(userDosePreference) && session.hasDose1Capacity();
    }

    private boolean sessionAndUserValidForDose2(Session session, Dose userDosePreference) {
        return Dose.DOSE_2.equals(userDosePreference) && session.hasDose2Capacity();
    }

    private boolean sessionAndUserValidForCovishield(Session session, Vaccine userVaccinePreference) {
        return Vaccine.COVISHIELD.equals(userVaccinePreference) && session.hasCovishield();
    }

    private boolean sessionAndUserValidForCovaxin(Session session, Vaccine userVaccinePreference) {
        return Vaccine.COVAXIN.equals(userVaccinePreference) && session.hasCovaxin();
    }

    private boolean sessionAndUserValidForSputnikV(Session session, Vaccine userVaccinePreference) {
        return Vaccine.SPUTNIK_V.equals(userVaccinePreference) && session.hasSputnikV();
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
