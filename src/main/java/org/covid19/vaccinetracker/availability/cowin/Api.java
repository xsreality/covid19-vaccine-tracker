package org.covid19.vaccinetracker.availability.cowin;

import org.covid19.vaccinetracker.availability.PriorityDistrictsAvailability;
import org.covid19.vaccinetracker.availability.VaccineAvailability;
import org.covid19.vaccinetracker.model.UsersByPincode;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.notifications.DistrictNotifications;
import org.covid19.vaccinetracker.notifications.VaccineCentersNotification;
import org.covid19.vaccinetracker.notifications.absentalerts.AbsentAlertNotifications;
import org.covid19.vaccinetracker.notifications.bot.BotService;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.covid19.vaccinetracker.userrequests.reconciliation.PincodeReconciliation;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

import static java.util.Collections.emptyList;
import static org.covid19.vaccinetracker.userrequests.model.Dose.DOSE_BOTH;

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
    private final BotService botService;
    private final PriorityDistrictsAvailability priorityDistrictsAvailability;
    private final AbsentAlertNotifications absentAlertNotifications;

    public Api(CowinApiClient cowinApiClient, VaccineAvailability vaccineAvailability, VaccineCentersNotification notifications,
               VaccinePersistence vaccinePersistence, UserRequestManager userRequestManager, PincodeReconciliation pincodeReconciliation, DistrictNotifications districtNotifications, CowinApiAuth cowinApiAuth, BotService botService, PriorityDistrictsAvailability priorityDistrictsAvailability, AbsentAlertNotifications absentAlertNotifications) {
        this.cowinApiClient = cowinApiClient;
        this.vaccineAvailability = vaccineAvailability;
        this.notifications = notifications;
        this.vaccinePersistence = vaccinePersistence;
        this.userRequestManager = userRequestManager;
        this.pincodeReconciliation = pincodeReconciliation;
        this.districtNotifications = districtNotifications;
        this.cowinApiAuth = cowinApiAuth;
        this.botService = botService;
        this.priorityDistrictsAvailability = priorityDistrictsAvailability;
        this.absentAlertNotifications = absentAlertNotifications;
    }

    @GetMapping("/fetch/cowin")
    public ResponseEntity<VaccineCenters> vaccineCenters(@RequestParam final String pincode) {
        return ResponseEntity.ok(cowinApiClient.fetchCentersByPincode(pincode));
    }

    @GetMapping("/fetch/user_request")
    public ResponseEntity<List<UserRequest>> fetchUserRequests() {
        return ResponseEntity.ok(userRequestManager.fetchAllUserRequests());
    }

    @GetMapping("/fetch/users_by_pincodes")
    public ResponseEntity<List<UsersByPincode>> fetchUsersByPincode() {
        return ResponseEntity.ok(userRequestManager.fetchAllUsersByPincode());
    }

    @GetMapping("/fetch/user_districts")
    public ResponseEntity<Set<District>> fetchUserDistricts() {
        return ResponseEntity.ok(userRequestManager.fetchAllUserDistricts());
    }

    @GetMapping("/fetch/missingPincodes")
    public ResponseEntity<List<String>> fetchMissingPincodes() {
        return ResponseEntity.ok(userRequestManager.missingPincodes());
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

    @PostMapping("/reload/user_request")
    public ResponseEntity<?> reloadUserRequests() {
        this.userRequestManager.regenerateUserRequests();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/add/user_request")
    public ResponseEntity<?> addUserRequest(@RequestParam final String chatId, @RequestParam final String pincodes) {
        if (pincodes.trim().isEmpty()) {
            this.userRequestManager.acceptUserRequest(chatId, List.of(), DOSE_BOTH.toString());
        } else {
            this.userRequestManager.acceptUserRequest(chatId, Utils.splitPincodes(pincodes), DOSE_BOTH.toString());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/add/user_district")
    public ResponseEntity<?> addUserDistrict(@RequestParam final String chatId, @RequestParam final String districts) {
        if (districts.trim().isEmpty()) {
            this.userRequestManager.updateDistrictPreference(chatId, List.of());
        } else {
            this.userRequestManager.updateDistrictPreference(chatId, Utils.splitDistricts(districts));
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/user_request")
    public ResponseEntity<?> removeUserRequest(@RequestParam final String chatId) {
        this.userRequestManager.acceptUserRequest(chatId, emptyList());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/vaccine_centers")
    public ResponseEntity<?> cleanupVaccineCentersByDate(@RequestParam String date) {
        this.vaccinePersistence.cleanupOldCenters(date);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger/updateViaLambda")
    public ResponseEntity<?> triggerCowinUpdatesViaLambda() {
        Executors.newSingleThreadExecutor().submit(this.vaccineAvailability::refreshVaccineAvailabilityFromCowinViaLambdaAsync);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger/priority_districts")
    public ResponseEntity<?> triggerPriorityDistrictsAvailability() {
        Executors.newSingleThreadExecutor().submit(priorityDistrictsAvailability::refreshPriorityDistrictsAvailabilityFromCowinViaLambdaAsync);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger/reconcile")
    public ResponseEntity<?> triggerPincodesReconciliation() {
        Executors.newSingleThreadExecutor().submit(() -> this.pincodeReconciliation.reconcilePincodesFromLambda(userRequestManager.fetchAllUserRequests()));
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

    @PostMapping("/bot/owner")
    public ResponseEntity<?> notifyOwner(@RequestBody String body) {
        this.botService.notifyOwner(body);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bot/all")
    public ResponseEntity<?> broadcast(@RequestBody String body) {
        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        Executors.newSingleThreadExecutor().submit(() -> {
            userRequestManager.fetchAllUserRequests()
                    .stream().filter(userRequest -> !userRequest.getPincodes().isEmpty())
                    .forEach(userRequest -> {
                        if (this.botService.notify(userRequest.getChatId(), body)) {
                            total.incrementAndGet();
                        } else {
                            failed.incrementAndGet();
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    });
            log.info("Broadcast completed. Total: {}, Failed: {}", total.get(), failed.get());
            this.botService.notifyOwner(String.format("Broadcast completed. Total: %d, Failed: %d", total.get(), failed.get()));
        });
        return ResponseEntity.ok().build();
    }

    @GetMapping("/absent_alerts")
    public ResponseEntity<?> alertAbsentCause(@RequestParam final String userId) {
        return ResponseEntity.ok(absentAlertNotifications.onDemandAbsentAlertsNotification(userId));
    }
}
