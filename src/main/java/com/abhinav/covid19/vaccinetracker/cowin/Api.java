package com.abhinav.covid19.vaccinetracker.cowin;

import com.abhinav.covid19.vaccinetracker.model.VaccineCenters;
import com.abhinav.covid19.vaccinetracker.tracker.VaccineTracker;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cowin")
public class Api {
    private final CowinApiClient cowinApiClient;
    private final VaccineTracker vaccineTracker;

    public Api(CowinApiClient cowinApiClient, VaccineTracker vaccineTracker) {
        this.cowinApiClient = cowinApiClient;
        this.vaccineTracker = vaccineTracker;
    }

    @GetMapping("/fetch")
    public ResponseEntity<VaccineCenters> vaccineCenters(@RequestParam final String pincode) {
        return ResponseEntity.ok(cowinApiClient.fetchCentersByPincode(pincode));
    }

    @GetMapping("/fetch/store")
    public ResponseEntity<VaccineCenters> vaccineCentersFromStore(@RequestParam final String pincode) {
        return ResponseEntity.ok(vaccineTracker.fetchVaccineAvailability(pincode));
    }

    @GetMapping("/persist")
    public ResponseEntity<?> tracker(@RequestParam final String pincode) {
        vaccineTracker.refreshVaccineAvailability(pincode);
        return ResponseEntity.ok().build();
    }
}
