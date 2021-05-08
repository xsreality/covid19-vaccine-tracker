package org.covid19.vaccinetracker.cowin;

import org.covid19.vaccinetracker.availability.VaccineAvailability;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.notifications.VaccineCentersNotification;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.reconciliation.PincodeReconciliation;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/covid19")
public class Api {
    private final CowinApiClient cowinApiClient;
    private final VaccineAvailability vaccineAvailability;
    private final VaccineCentersNotification notifications;
    private final VaccinePersistence vaccinePersistence;
    private final UserRequestManager userRequestManager;
    private final PincodeReconciliation pincodeReconciliation;

    public Api(CowinApiClient cowinApiClient, VaccineAvailability vaccineAvailability, VaccineCentersNotification notifications,
               VaccinePersistence vaccinePersistence, UserRequestManager userRequestManager, PincodeReconciliation pincodeReconciliation) {
        this.cowinApiClient = cowinApiClient;
        this.vaccineAvailability = vaccineAvailability;
        this.notifications = notifications;
        this.vaccinePersistence = vaccinePersistence;
        this.userRequestManager = userRequestManager;
        this.pincodeReconciliation = pincodeReconciliation;
    }

    @GetMapping("/fetch/cowin")
    public ResponseEntity<VaccineCenters> vaccineCenters(@RequestParam final String pincode) {
        return ResponseEntity.ok(cowinApiClient.fetchCentersByPincode(pincode));
    }

    @GetMapping("/fetch/vaccine_centers_by_pincode")
    public ResponseEntity<VaccineCenters> vaccineCentersFromStore(@RequestParam final String pincode) {
        return ResponseEntity.ok(vaccineAvailability.fetchVaccineAvailabilityFromPersistenceStore(pincode));
    }

    @GetMapping("/fetch/user_request")
    public ResponseEntity<List<UserRequest>> fetchUserRequests() {
        return ResponseEntity.ok(userRequestManager.fetchAllUserRequests());
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

    @PostMapping("/notify/cowin")
    public ResponseEntity<?> triggerNotificationsFromCowinApi() {
        this.notifications.checkUpdatesDirectlyWithCowinAndSendNotifications();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/add/user_request")
    public ResponseEntity<?> addUserRequest(@RequestParam final String chatId, @RequestParam final String pincodes) {
        this.userRequestManager.acceptUserRequest(chatId, Utils.splitPincodes(pincodes));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/add/dummy_vaccine_center")
    public ResponseEntity<?> addUserRequest(@RequestParam final String pincode) {
        Center center = Center.builder().centerId(123456).name("Test Vaccine Center").stateName("state").districtName("district").pincode(Integer.parseInt(pincode))
                .sessions(Collections.singletonList(Session.builder().vaccine("COVISHIELD").sessionId("xxx").date("04-05-2021").minAgeLimit(18).availableCapacity(5).build()))
                .build();
        VaccineCenters vaccineCenters = new VaccineCenters();
        vaccineCenters.setCenters(Collections.singletonList(center));
        this.vaccinePersistence.persistVaccineCenters(pincode, vaccineCenters);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger/update")
    public ResponseEntity<?> triggerCowinUpdates() {
        Executors.newSingleThreadExecutor().submit(this.vaccineAvailability::refreshVaccineAvailabilityFromCowin);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger/reconcile")
    public ResponseEntity<?> triggerPincodesReconciliation() {
        Executors.newSingleThreadExecutor().submit(() -> this.pincodeReconciliation.reconcilePincodesFromCowin(userRequestManager.fetchAllUserRequests()));
        return ResponseEntity.ok().build();
    }
}
