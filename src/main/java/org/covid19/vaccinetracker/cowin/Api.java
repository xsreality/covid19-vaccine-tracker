package org.covid19.vaccinetracker.cowin;

import org.covid19.vaccinetracker.availability.VaccineAvailability;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.notifications.VaccineCentersNotification;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/covid19")
public class Api {
    private final CowinApiClient cowinApiClient;
    private final VaccineAvailability vaccineAvailability;
    private final VaccineCentersNotification notifications;
    private final UserRequestManager userRequestManager;

    public Api(CowinApiClient cowinApiClient, VaccineAvailability vaccineAvailability, VaccineCentersNotification notifications, UserRequestManager userRequestManager) {
        this.cowinApiClient = cowinApiClient;
        this.vaccineAvailability = vaccineAvailability;
        this.notifications = notifications;
        this.userRequestManager = userRequestManager;
    }

    @GetMapping("/fetch/cowin")
    public ResponseEntity<VaccineCenters> vaccineCenters(@RequestParam final String pincode) {
        return ResponseEntity.ok(cowinApiClient.fetchCentersByPincode(pincode));
    }

    @GetMapping("/fetch/store")
    public ResponseEntity<VaccineCenters> vaccineCentersFromStore(@RequestParam final String pincode) {
        return ResponseEntity.ok(vaccineAvailability.fetchVaccineAvailabilityFromPersistenceStore(pincode));
    }

    @GetMapping("/persist/cowin")
    public ResponseEntity<?> tracker(@RequestParam final String pincode) {
        vaccineAvailability.fetchVaccineAvailabilityFromCowinApi(pincode);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notify")
    public ResponseEntity<?> triggerNotifications() {
        this.notifications.checkUpdatesAndSendNotifications();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user_request/add")
    public ResponseEntity<?> addUserRequest(@RequestParam final String chatId, @RequestParam final String pincodes) {
        this.userRequestManager.acceptUserRequest(chatId, Utils.splitPincodes(pincodes));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger/update")
    public ResponseEntity<?> triggerCowinUpdates() {
        this.vaccineAvailability.refreshVaccineAvailabilityFromCowin();
        return ResponseEntity.ok().build();
    }
}
