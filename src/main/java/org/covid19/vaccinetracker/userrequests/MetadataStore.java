package org.covid19.vaccinetracker.userrequests;

import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.userrequests.model.Pincode;
import org.covid19.vaccinetracker.persistence.mariadb.repository.DistrictRepository;
import org.covid19.vaccinetracker.persistence.mariadb.repository.PincodeRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MetadataStore {
    private final DistrictRepository districtRepository;
    private final PincodeRepository pincodeRepository;

    public MetadataStore(DistrictRepository districtRepository, PincodeRepository pincodeRepository) {
        this.districtRepository = districtRepository;
        this.pincodeRepository = pincodeRepository;
    }

    public boolean pincodeExists(String pincode) {
        return this.pincodeRepository.existsByPincode(pincode);
    }

    public void persistPincode(Pincode pincode) {
        this.pincodeRepository.save(pincode);
    }

    public List<District> fetchDistrictsByPincode(String pincode) {
        return districtRepository.findDistrictByPincode(pincode);
    }

    public District fetchDistrictByNameAndState(String districtName, String stateName) {
        return this.districtRepository.findDistrictByDistrictNameAndState(districtName, stateName);
    }

    public List<Pincode> fetchPincodesByDistrictId(Integer districtId) {
        return this.pincodeRepository.findPincodeByDistrictId(districtId);
    }
}
