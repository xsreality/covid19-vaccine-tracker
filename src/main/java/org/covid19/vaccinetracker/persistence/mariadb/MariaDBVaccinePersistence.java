package org.covid19.vaccinetracker.persistence.mariadb;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.CenterEntity;
import org.covid19.vaccinetracker.persistence.mariadb.entity.District;
import org.covid19.vaccinetracker.persistence.mariadb.entity.Pincode;
import org.covid19.vaccinetracker.persistence.mariadb.entity.SessionEntity;
import org.covid19.vaccinetracker.persistence.mariadb.repository.CenterRepository;
import org.covid19.vaccinetracker.persistence.mariadb.repository.DistrictRepository;
import org.covid19.vaccinetracker.persistence.mariadb.repository.PincodeRepository;
import org.covid19.vaccinetracker.persistence.mariadb.repository.SessionRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MariaDBVaccinePersistence implements VaccinePersistence {
    private final CenterRepository centerRepository;
    private final SessionRepository sessionRepository;
    private final DistrictRepository districtRepository;
    private final PincodeRepository pincodeRepository;

    public MariaDBVaccinePersistence(CenterRepository centerRepository, SessionRepository sessionRepository,
                                     DistrictRepository districtRepository, PincodeRepository pincodeRepository) {
        this.centerRepository = centerRepository;
        this.sessionRepository = sessionRepository;
        this.districtRepository = districtRepository;
        this.pincodeRepository = pincodeRepository;
    }

    @Override
    public VaccineCenters fetchVaccineCentersByPincode(String pincode) {
        final List<CenterEntity> centerEntities = this.centerRepository.findCenterEntityByPincodeAndSessionsProcessedAtIsNull(pincode);
        return toVaccineCenters(centerEntities);
    }

    private VaccineCenters toVaccineCenters(List<CenterEntity> centerEntities) {
        final List<Center> centers = new ArrayList<>();
        centerEntities.forEach(centerEntity -> {
            final List<Session> sessions = new ArrayList<>();
            centerEntity.getSessions().forEach(sessionEntity -> sessions.add(Session.builder()
                    .sessionId(sessionEntity.getId())
                    .vaccine(sessionEntity.getVaccine())
                    .date(sessionEntity.getDate())
                    .availableCapacity(sessionEntity.getAvailableCapacity())
                    .minAgeLimit(sessionEntity.getMinAgeLimit())
                    .build()));
            centers.add(Center.builder()
                    .pincode(Integer.valueOf(centerEntity.getPincode()))
                    .name(centerEntity.getName())
                    .districtName(centerEntity.getDistrictName())
                    .stateName(centerEntity.getStateName())
                    .centerId((int) centerEntity.getId())
                    .feeType(centerEntity.getFeeType())
                    .sessions(sessions)
                    .build());
        });
        VaccineCenters vaccineCenters = new VaccineCenters();
        vaccineCenters.setCenters(centers);
        return vaccineCenters;
    }

    @Override
    public List<VaccineCenters> fetchAllVaccineCenters() {
        return null;
    }

    @Override
    public List<District> fetchDistrictsByPincode(String pincode) {
        return districtRepository.findDistrictByPincode(pincode);
    }

    @Override
    public void persistVaccineCenters(VaccineCenters vaccineCenters) {
        centerRepository.saveAll(toCenterEntities(vaccineCenters, null));
    }

    @Override
    public void markProcessed(VaccineCenters vaccineCenters) {
        vaccineCenters.getCenters().forEach(center -> {
            Set<SessionEntity> sessionEntities = new HashSet<>();
            center.getSessions().forEach(session -> sessionEntities.add(SessionEntity.builder()
                    .id(session.sessionId)
                    .date(session.date)
                    .vaccine(session.vaccine)
                    .availableCapacity(session.availableCapacity)
                    .minAgeLimit(session.minAgeLimit)
                    .processedAt(LocalDateTime.now())
                    .build()));
            sessionRepository.saveAll(sessionEntities);
        });
    }

    @NotNull
    private Set<CenterEntity> toCenterEntities(VaccineCenters vaccineCenters, @SuppressWarnings("SameParameterValue") LocalDateTime processedAt) {
        Set<CenterEntity> centerEntities = new HashSet<>();
        vaccineCenters.getCenters().forEach(center -> {
            Set<SessionEntity> sessionEntities = new HashSet<>();
            center.getSessions().forEach(session -> sessionEntities.add(SessionEntity.builder()
                    .id(session.sessionId)
                    .date(session.date)
                    .vaccine(session.vaccine)
                    .availableCapacity(session.availableCapacity)
                    .minAgeLimit(session.minAgeLimit)
                    .processedAt(processedAt)
                    .build()));
            sessionRepository.saveAll(sessionEntities);
            centerEntities.add(CenterEntity.builder()
                    .id(center.getCenterId())
                    .name(center.getName())
                    .districtName(center.getDistrictName())
                    .stateName(center.getStateName())
                    .feeType(center.getFeeType())
                    .sessions(sessionEntities)
                    .pincode(String.valueOf(center.getPincode()))
                    .build());
        });
        return centerEntities;
    }

    @Override
    public boolean pincodeExists(String pincode) {
        return this.pincodeRepository.existsByPincode(pincode);
    }

    @Override
    public District fetchDistrictByNameAndState(String districtName, String stateName) {
        return this.districtRepository.findDistrictByDistrictNameAndState(districtName, stateName);
    }

    @Override
    public void persistPincode(Pincode pincode) {
        this.pincodeRepository.save(pincode);
    }
}
