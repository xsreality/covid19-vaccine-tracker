package org.covid19.vaccinetracker.persistence.mariadb.repository;

import org.covid19.vaccinetracker.persistence.mariadb.entity.Pincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PincodeRepository extends CrudRepository<Pincode, String> {
    List<Pincode> findPincodeByDistrictId(int districtId);

    boolean existsByPincode(String pincode);
}
