package org.covid19.vaccinetracker.reconciliation;

import org.covid19.vaccinetracker.bot.BotService;
import org.covid19.vaccinetracker.cowin.CowinApiClient;
import org.covid19.vaccinetracker.model.UserRequest;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.persistence.VaccinePersistence;
import org.covid19.vaccinetracker.persistence.mariadb.entity.District;
import org.covid19.vaccinetracker.persistence.mariadb.entity.Pincode;
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
    private final BotService botService;

    public PincodeReconciliation(VaccinePersistence vaccinePersistence, CowinApiClient cowinApiClient, ReconciliationStats reconciliationStats, BotService botService) {
        this.vaccinePersistence = vaccinePersistence;
        this.cowinApiClient = cowinApiClient;
        this.reconciliationStats = reconciliationStats;
        this.botService = botService;
    }

    public void reconcilePincodesFromCowin(List<UserRequest> userRequests) {
        log.info("Starting reconciliation of missing pincodes from Cowin API");
        reconciliationStats.reset();
        final List<String> processedPincodes = new ArrayList<>();
        userRequests.forEach(userRequest -> {
            final List<String> pincodes = userRequest.getPincodes();
            pincodes.forEach(pincode -> {
                log.info("Processing pincode {}", pincode);
                if (processedPincodes.contains(pincode)) {
                    log.info("Pincode {} processed already, skipping...", pincode);
                    return;
                }
                processedPincodes.add(pincode);
                if (vaccinePersistence.pincodeExists(pincode)) {    // no reconciliation needed
                    log.info("No reconciliation needed for pincode {}", pincode);
                    return;
                }
                log.warn("Pincode {} not found in DB. Reconiliing...", pincode);
                reconciliationStats.incrementUnknownPincodes();
                final VaccineCenters vaccineCenters = cowinApiClient.fetchCentersByPincode(pincode);
                introduceDelay();
                if (isNull(vaccineCenters) || vaccineCenters.centers.isEmpty()) {
                    log.info("Failed reconciliation for pincode {}", pincode);
                    reconciliationStats.incrementFailedReconciliations();
                    return;
                }
                String districtName = vaccineCenters.getCenters().get(0).getDistrictName();
                District district = vaccinePersistence.fetchDistrictByName(districtName);
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
        log.info("Pincode reconciliation = Unknown: {}, Failed: {}, Missing district: {}, Successful: {}",
                reconciliationStats.unknownPincodes(), reconciliationStats.failedReconciliations(),
                reconciliationStats.failedWithUnknownDistrict(), reconciliationStats.successfulReconciliations());
        botService.notifyOwner(String.format("Pincode reconciliation = Unknown: %d, Failed: %d, Missing district: %d, Successful: %d",
                reconciliationStats.unknownPincodes(), reconciliationStats.failedReconciliations(),
                reconciliationStats.failedWithUnknownDistrict(), reconciliationStats.successfulReconciliations()));
    }

    private void introduceDelay() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // eat
        }
    }
}
