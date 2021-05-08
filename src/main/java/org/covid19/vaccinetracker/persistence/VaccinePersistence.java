package org.covid19.vaccinetracker.persistence;

import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.mariadb.entity.District;
import org.covid19.vaccinetracker.persistence.mariadb.entity.Pincode;

import java.util.ArrayList;
import java.util.List;

public interface VaccinePersistence {
    default void persistVaccineCenters(String pincode, VaccineCenters vaccineCenters) {
        // noop
    }

    default void persistVaccineCenters(VaccineCenters vaccineCenters) {
        // noop
    }

    void persistPincode(Pincode pincode);

    boolean pincodeExists(String pincode);

    District fetchDistrictByName(String districtName);

    VaccineCenters fetchVaccineCentersByPincode(String pincode);

    default List<District> fetchDistrictsByPincode(String pincode) {
        return new ArrayList<>();
    }

    List<VaccineCenters> fetchAllVaccineCenters();
}
