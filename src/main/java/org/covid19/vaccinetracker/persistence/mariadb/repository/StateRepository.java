package org.covid19.vaccinetracker.persistence.mariadb.repository;

import org.covid19.vaccinetracker.userrequests.model.State;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateRepository extends CrudRepository<State, Integer> {
    @Query("SELECT s FROM State s " +
            "INNER JOIN District d ON d.state = s " +
            "INNER JOIN Pincode p ON p.district = d " +
            "WHERE p.pincode = :pincode")
    State findByPincode(String pincode);
}
