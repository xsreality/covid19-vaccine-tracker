package org.covid19.vaccinetracker.persistence.mariadb;

import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.CenterSession;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.CenterEntity;
import org.covid19.vaccinetracker.persistence.mariadb.entity.SessionEntity;
import org.covid19.vaccinetracker.persistence.mariadb.repository.CenterRepository;
import org.covid19.vaccinetracker.persistence.mariadb.repository.SessionRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.nonNull;

@Slf4j
@Component
public class MariaDBVaccinePersistence implements VaccinePersistence {
    private final CenterRepository centerRepository;
    private final SessionRepository sessionRepository;

    public MariaDBVaccinePersistence(CenterRepository centerRepository, SessionRepository sessionRepository) {
        this.centerRepository = centerRepository;
        this.sessionRepository = sessionRepository;
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
                    .availableCapacityDose1(sessionEntity.getAvailableCapacityDose1())
                    .availableCapacityDose2(sessionEntity.getAvailableCapacityDose2())
                    .cost(sessionEntity.getCost())
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
                    .availableCapacity(nonNull(session.availableCapacity) ? session.availableCapacity : 0)
                    .availableCapacityDose1(nonNull(session.availableCapacityDose1) ? session.availableCapacityDose1 : 0)
                    .availableCapacityDose2(nonNull(session.availableCapacityDose2) ? session.availableCapacityDose2 : 0)
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
                    .availableCapacity(nonNull(session.availableCapacity) ? session.availableCapacity : 0)
                    .availableCapacityDose1(nonNull(session.availableCapacityDose1) ? session.availableCapacityDose1 : 0)
                    .availableCapacityDose2(nonNull(session.availableCapacityDose2) ? session.availableCapacityDose2 : 0)
                    .cost(nonNull(center.feeType) ? (center.paid() ? center.costFor(session.vaccine) : "Free") : null)
                    .minAgeLimit(session.minAgeLimit)
                    .processedAt(session.shouldNotify ? processedAt : LocalDateTime.now())
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

    @Transactional
    @Override
    public void cleanupOldCenters(String date) {
        this.centerRepository.deleteBySessionsDate(date);
    }

    @Override
    public Optional<SessionEntity> findExistingSession(Long centerId, String date, Integer age, String vaccine) {
        return sessionRepository.findLatestSession(centerId, date, age, vaccine)
                .stream()
                .findFirst();
    }

    @Override
    public List<CenterSession> findAllSessionsByPincode(String pincode) {
        return sessionRepository.findSessionsWithPincode(pincode);
    }
}
