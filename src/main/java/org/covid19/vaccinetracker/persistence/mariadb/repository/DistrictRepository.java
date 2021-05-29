package org.covid19.vaccinetracker.persistence.mariadb.repository;

import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.userrequests.model.State;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepository extends CrudRepository<District, Integer> {
    List<District> findDistrictByState(State state);

    @Query("SELECT d FROM District d " +
            "INNER JOIN State s ON d.state = s " +
            "WHERE d.districtName = :name " +
            "AND s.stateName = :stateName")
    District findDistrictByDistrictNameAndState(String name, String stateName);

    @Query("SELECT p.district FROM Pincode p WHERE p.pincode = :pincode")
    List<District> findDistrictByPincode(String pincode);
}
