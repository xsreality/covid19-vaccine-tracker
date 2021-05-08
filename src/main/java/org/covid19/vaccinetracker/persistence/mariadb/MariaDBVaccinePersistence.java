package org.covid19.vaccinetracker.persistence.mariadb;

import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.CenterEntity;
import org.covid19.vaccinetracker.persistence.mariadb.entity.District;
import org.covid19.vaccinetracker.persistence.mariadb.entity.SessionEntity;
import org.covid19.vaccinetracker.persistence.mariadb.repository.CenterRepository;
import org.covid19.vaccinetracker.persistence.mariadb.repository.DistrictRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MariaDBVaccinePersistence implements VaccinePersistence {
    private final CenterRepository centerRepository;
    private final DistrictRepository districtRepository;

    public MariaDBVaccinePersistence(CenterRepository centerRepository, DistrictRepository districtRepository) {
        this.centerRepository = centerRepository;
        this.districtRepository = districtRepository;
    }

    @Override
    public VaccineCenters fetchVaccineCentersByPincode(String pincode) {
        return null;
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
        Set<CenterEntity> centerEntities = new HashSet<>();
        vaccineCenters.getCenters().forEach(center -> {
            Set<SessionEntity> sessionEntities = new HashSet<>();
            center.getSessions().forEach(session -> sessionEntities.add(SessionEntity.builder()
                    .id(session.sessionId)
                    .date(session.date)
                    .vaccine(session.vaccine)
                    .availableCapacity(session.availableCapacity)
                    .minAgeLimit(session.minAgeLimit)
                    .build()));
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

        centerRepository.saveAll(centerEntities);
    }
}