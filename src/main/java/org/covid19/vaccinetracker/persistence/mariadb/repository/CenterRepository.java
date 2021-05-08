package org.covid19.vaccinetracker.persistence.mariadb.repository;

import org.covid19.vaccinetracker.persistence.mariadb.entity.CenterEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CenterRepository extends CrudRepository<CenterEntity, Long> {
    List<CenterEntity> findCenterEntityByPincode(String pincode);
}
