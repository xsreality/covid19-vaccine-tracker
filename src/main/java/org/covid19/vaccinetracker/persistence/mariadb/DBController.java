package org.covid19.vaccinetracker.persistence.mariadb;

import org.covid19.vaccinetracker.availability.VaccineAvailability;
import org.covid19.vaccinetracker.persistence.mariadb.entity.State;
import org.covid19.vaccinetracker.persistence.mariadb.repository.DistrictRepository;
import org.covid19.vaccinetracker.persistence.mariadb.repository.PincodeRepository;
import org.covid19.vaccinetracker.persistence.mariadb.repository.StateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/db")
public class DBController {
    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final PincodeRepository pincodeRepository;
    private final VaccineAvailability vaccineAvailability;

    public DBController(StateRepository stateRepository, DistrictRepository districtRepository, PincodeRepository pincodeRepository, VaccineAvailability vaccineAvailability) {
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.pincodeRepository = pincodeRepository;
        this.vaccineAvailability = vaccineAvailability;
    }

    @GetMapping("/states/all")
    public ResponseEntity<?> fetchAllStates() {
        return ResponseEntity.ok(this.stateRepository.findAll());
    }

    @GetMapping("/districts/all")
    public ResponseEntity<?> fetchAllDistricts() {
        return ResponseEntity.ok(this.districtRepository.findAll());
    }

    @GetMapping("/districts/byState")
    public ResponseEntity<?> fetchDistrictByState(@RequestParam State state) {
        return ResponseEntity.ok(this.districtRepository.findDistrictByState(state));
    }

    @GetMapping("/pincodes/all")
    public ResponseEntity<?> fetchAllPincodes() {
        return ResponseEntity.ok(this.pincodeRepository.findAll());
    }

    @GetMapping("/pincodes/byDistrictId")
    public ResponseEntity<?> fetchPincodesByDistrict(@RequestParam int districtId) {
        return ResponseEntity.ok(this.pincodeRepository.findPincodeByDistrictId(districtId));
    }

    @GetMapping("/districts/byPincode")
    public ResponseEntity<?> fetchDistrictByPincode(@RequestParam String pincode) {
        return ResponseEntity.ok(this.districtRepository.findDistrictByPincode(pincode));
    }

    @PostMapping("/sessions/byDistrict")
    public ResponseEntity<?> saveSessionsByDistrict(@RequestParam int districtId) {
        vaccineAvailability.refreshVaccineAvailabilityFromCowinApi(districtId);
        return ResponseEntity.ok().build();
    }
}
