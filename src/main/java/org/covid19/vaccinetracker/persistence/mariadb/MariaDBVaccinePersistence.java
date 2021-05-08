package org.covid19.vaccinetracker.persistence.mariadb;

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
import org.springframework.stereotype.Component;

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

    public MariaDBVaccinePersistence(CenterRepository centerRepository, SessionRepository sessionRepository, DistrictRepository districtRepository,
                                     PincodeRepository pincodeRepository) {
        this.centerRepository = centerRepository;
        this.sessionRepository = sessionRepository;
        this.districtRepository = districtRepository;
        this.pincodeRepository = pincodeRepository;
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
        centerRepository.saveAll(centerEntities);
    }

    @Override
    public boolean pincodeExists(String pincode) {
        return this.pincodeRepository.existsByPincode(pincode);
    }

    @Override
    public District fetchDistrictByName(String districtName) {
        return this.districtRepository.findDistrictByDistrictName(districtName);
    }

    @Override
    public void persistPincode(Pincode pincode) {
        this.pincodeRepository.save(pincode);
    }
}
