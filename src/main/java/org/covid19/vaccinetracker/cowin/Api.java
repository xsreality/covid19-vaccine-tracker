package org.covid19.vaccinetracker.cowin;

import org.covid19.vaccinetracker.availability.VaccineAvailability;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.notifications.DistrictNotifications;
import org.covid19.vaccinetracker.notifications.VaccineCentersNotification;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.District;
import org.covid19.vaccinetracker.reconciliation.PincodeReconciliation;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

import static java.util.Collections.emptyList;

@Slf4j
@RestController
@RequestMapping("/covid19")
public class Api {
    private final CowinApiClient cowinApiClient;
    private final VaccineAvailability vaccineAvailability;
    private final VaccineCentersNotification notifications;
    private final VaccinePersistence vaccinePersistence;
    private final UserRequestManager userRequestManager;
    private final PincodeReconciliation pincodeReconciliation;
    private final DistrictNotifications districtNotifications;
    private final CowinApiAuth cowinApiAuth;

    public Api(CowinApiClient cowinApiClient, VaccineAvailability vaccineAvailability, VaccineCentersNotification notifications,
               VaccinePersistence vaccinePersistence, UserRequestManager userRequestManager, PincodeReconciliation pincodeReconciliation, DistrictNotifications districtNotifications, CowinApiAuth cowinApiAuth) {
        this.cowinApiClient = cowinApiClient;
        this.vaccineAvailability = vaccineAvailability;
        this.notifications = notifications;
        this.vaccinePersistence = vaccinePersistence;
        this.userRequestManager = userRequestManager;
        this.pincodeReconciliation = pincodeReconciliation;
        this.districtNotifications = districtNotifications;
        this.cowinApiAuth = cowinApiAuth;
    }

    @GetMapping("/fetch/cowin")
    public ResponseEntity<VaccineCenters> vaccineCenters(@RequestParam final String pincode) {
        return ResponseEntity.ok(cowinApiClient.fetchCentersByPincode(pincode));
    }

    @GetMapping("/fetch/user_request")
    public ResponseEntity<List<UserRequest>> fetchUserRequests() {
        return ResponseEntity.ok(userRequestManager.fetchAllUserRequests());
    }

    @GetMapping("/fetch/user_districts")
    public ResponseEntity<Set<District>> fetchUserDistricts() {
        return ResponseEntity.ok(userRequestManager.fetchAllUserDistricts());
    }

    @GetMapping("/fetch/missingPincodes")
    public ResponseEntity<List<String>> fetchMissingPincodes() {
        return ResponseEntity.ok(vaccineAvailability.missingPincodes());
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

    @PostMapping("/notify/db")
    public ResponseEntity<?> triggerNotificationsFromDB() {
        this.notifications.checkUpdatesAndSendNotifications();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notify/district")
    public ResponseEntity<?> triggerDistrictNotifications() {
        this.districtNotifications.sendDistrictNotifications();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/add/user_request")
    public ResponseEntity<?> addUserRequest(@RequestParam final String chatId, @RequestParam final String pincodes) {
        this.userRequestManager.acceptUserRequest(chatId, Utils.splitPincodes(pincodes));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/user_request")
    public ResponseEntity<?> removeUserRequest(@RequestParam final String chatId) {
        this.userRequestManager.acceptUserRequest(chatId, emptyList());
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

    @DeleteMapping("/delete/vaccine_centers")
    public ResponseEntity<?> cleanupVaccineCentersByDate(@RequestParam String date) {
        this.vaccinePersistence.cleanupOldCenters(date);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger/updateViaKafka")
    public ResponseEntity<?> triggerCowinUpdatesViaKafka() {
        Executors.newSingleThreadExecutor().submit(this.vaccineAvailability::refreshVaccineAvailabilityFromCowinViaKafka);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger/reconcile")
    public ResponseEntity<?> triggerPincodesReconciliation() {
        Executors.newSingleThreadExecutor().submit(() -> this.pincodeReconciliation.reconcilePincodesFromCowin(userRequestManager.fetchAllUserRequests()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody String callback) {
        log.info("Received a call on webhook endpoint with text: {}", callback);
        Executors.newSingleThreadExecutor().submit(() -> this.cowinApiAuth.handleOtpCallback(callback));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/generateOtp")
    public ResponseEntity<?> generateOtp() {
        Executors.newSingleThreadExecutor().submit(this.cowinApiAuth::refreshCowinToken);
        return ResponseEntity.ok().build();
    }
}
