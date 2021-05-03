package com.abhinav.covid19.vaccinetracker.cowin;

import com.abhinav.covid19.vaccinetracker.VaccineCenters;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cowin")
public class Api {
    private final CowinService cowinService;

    public Api(CowinService cowinService) {
        this.cowinService = cowinService;
    }

    @GetMapping
    public ResponseEntity<VaccineCenters> vaccineCenters(@RequestParam @NonNull final String pincode) {
        return ResponseEntity.ok(cowinService.fetchCentersByPincode(pincode));
    }
}
