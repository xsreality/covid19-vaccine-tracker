package org.covid19.vaccinetracker.persistence.mariadb.repository;

import org.covid19.vaccinetracker.persistence.mariadb.entity.District;
import org.covid19.vaccinetracker.persistence.mariadb.entity.State;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepository extends CrudRepository<District, Integer> {
    List<District> findDistrictByState(State state);

    @Query("SELECT p.district FROM Pincode p WHERE p.pincode = :pincode")
    List<District> findDistrictByPincode(String pincode);
}
