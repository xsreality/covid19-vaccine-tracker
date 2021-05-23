package org.covid19.vaccinetracker.reconciliation;

import org.covid19.vaccinetracker.bot.BotService;
import org.covid19.vaccinetracker.cowin.CowinApiClient;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.District;
import org.covid19.vaccinetracker.persistence.mariadb.entity.Pincode;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;

@Slf4j
@Service
public class PincodeReconciliation {
    private final VaccinePersistence vaccinePersistence;
    private final CowinApiClient cowinApiClient;
    private final ReconciliationStats reconciliationStats;
    private final UserRequestManager userRequestManager;
    private final BotService botService;

    public PincodeReconciliation(VaccinePersistence vaccinePersistence, CowinApiClient cowinApiClient,
                                 ReconciliationStats reconciliationStats, UserRequestManager userRequestManager,
                                 BotService botService) {
        this.vaccinePersistence = vaccinePersistence;
        this.cowinApiClient = cowinApiClient;
        this.reconciliationStats = reconciliationStats;
        this.userRequestManager = userRequestManager;
        this.botService = botService;
    }

    @Scheduled(cron = "${jobs.cron.pincode.reconciliation:-}", zone = "IST")
    public void pincodesReconciliationJob() {
        this.reconcilePincodesFromCowin(userRequestManager.fetchAllUserRequests());
    }

    public void reconcilePincodesFromCowin(List<UserRequest> userRequests) {
        log.info("Starting reconciliation of missing pincodes from Cowin API");
        final List<String> processedPincodes = new ArrayList<>();
        reconciliationStats.reset();
        reconciliationStats.noteStartTime();
        userRequests.forEach(userRequest -> {
            final List<String> pincodes = userRequest.getPincodes();
            pincodes.forEach(pincode -> {
                log.debug("Processing pincode {}", pincode);
                if (processedPincodes.contains(pincode)) {
                    log.debug("Pincode {} processed already, skipping...", pincode);
                    return;
                }
                processedPincodes.add(pincode);
                if (vaccinePersistence.pincodeExists(pincode)) {    // no reconciliation needed
                    log.debug("No reconciliation needed for pincode {}", pincode);
                    return;
                }
                log.debug("Pincode {} not found in DB. Reconciling...", pincode);
                reconciliationStats.incrementUnknownPincodes();
                final VaccineCenters vaccineCenters = cowinApiClient.fetchCentersByPincode(pincode);
                introduceDelay();
                if (isNull(vaccineCenters) || vaccineCenters.centers.isEmpty()) {
                    log.debug("Failed reconciliation for pincode {}", pincode);
                    reconciliationStats.incrementFailedReconciliations();
                    return;
                }
                String districtName = vaccineCenters.getCenters().get(0).getDistrictName();
                String stateName = vaccineCenters.getCenters().get(0).getStateName();
                District district = vaccinePersistence.fetchDistrictByNameAndState(districtName, stateName);
                if (isNull(district)) {
                    log.warn("No district found in DB for name {}", districtName);
                    reconciliationStats.incrementFailedWithUnknownDistrict();
                    return;
                }
                this.vaccinePersistence.persistPincode(Pincode.builder()
                        .pincode(pincode)
                        .district(district)
                        .build());
                log.info("Reconciliation successful for pincode {}", pincode);
                reconciliationStats.incrementSuccessfulReconciliations();
            });
        });
        reconciliationStats.noteEndTime();
        log.info("[PINCODE RECONCILIATION] Unknown: {}, Failed: {}, Missing district: {}, Successful: {}, Time taken: {}",
                reconciliationStats.unknownPincodes(), reconciliationStats.failedReconciliations(),
                reconciliationStats.failedWithUnknownDistrict(), reconciliationStats.successfulReconciliations(), reconciliationStats.timeTaken());
        botService.notifyOwner(String.format("[PINCODE RECONCILIATION] Unknown: %d, Failed: %d, Missing district: %d, Successful: %d, Time taken: %s",
                reconciliationStats.unknownPincodes(), reconciliationStats.failedReconciliations(),
                reconciliationStats.failedWithUnknownDistrict(), reconciliationStats.successfulReconciliations(), reconciliationStats.timeTaken()));
    }

    private void introduceDelay() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // eat
        }
    }
}
