package org.covid19.vaccinetracker.cowin;

import org.covid19.vaccinetracker.availability.VaccineAvailability;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.notifications.VaccineCentersNotification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cowin")
public class Api {
    private final CowinApiClient cowinApiClient;
    private final VaccineAvailability vaccineAvailability;
    private final VaccineCentersNotification notifications;

    public Api(CowinApiClient cowinApiClient, VaccineAvailability vaccineAvailability, VaccineCentersNotification notifications) {
        this.cowinApiClient = cowinApiClient;
        this.vaccineAvailability = vaccineAvailability;
        this.notifications = notifications;
    }

    @GetMapping("/fetch")
    public ResponseEntity<VaccineCenters> vaccineCenters(@RequestParam final String pincode) {
        return ResponseEntity.ok(cowinApiClient.fetchCentersByPincode(pincode));
    }

    @GetMapping("/fetch/store")
    public ResponseEntity<VaccineCenters> vaccineCentersFromStore(@RequestParam final String pincode) {
        return ResponseEntity.ok(vaccineAvailability.fetchVaccineAvailabilityFromPersistenceStore(pincode));
    }

    @GetMapping("/persist")
    public ResponseEntity<?> tracker(@RequestParam final String pincode) {
        vaccineAvailability.refreshVaccineAvailability(pincode);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notify")
    public ResponseEntity<?> triggerNotifications() {
        this.notifications.checkUpdatesAndSendNotifications();
        return ResponseEntity.ok().build();
    }
}
