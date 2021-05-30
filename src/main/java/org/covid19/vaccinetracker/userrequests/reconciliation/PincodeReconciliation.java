package org.covid19.vaccinetracker.userrequests.reconciliation;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.covid19.vaccinetracker.availability.aws.CowinLambdaWrapper;
import org.covid19.vaccinetracker.model.VaccineCenters;
import org.covid19.vaccinetracker.notifications.bot.BotService;
import org.covid19.vaccinetracker.userrequests.MetadataStore;
import org.covid19.vaccinetracker.userrequests.UserRequestManager;
import org.covid19.vaccinetracker.userrequests.model.District;
import org.covid19.vaccinetracker.userrequests.model.Pincode;
import org.covid19.vaccinetracker.userrequests.model.UserRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Service
public class PincodeReconciliation {
    private final MetadataStore metadataStore;
    private final CowinLambdaWrapper cowinLambdaWrapper;
    private final ReconciliationStats reconciliationStats;
    private final UserRequestManager userRequestManager;
    private final BotService botService;

    public PincodeReconciliation(MetadataStore metadataStore,
                                 CowinLambdaWrapper cowinLambdaWrapper, ReconciliationStats reconciliationStats, UserRequestManager userRequestManager,
                                 BotService botService) {
        this.metadataStore = metadataStore;
        this.cowinLambdaWrapper = cowinLambdaWrapper;
        this.reconciliationStats = reconciliationStats;
        this.userRequestManager = userRequestManager;
        this.botService = botService;
    }

    @Scheduled(cron = "${jobs.cron.pincode.reconciliation:-}", zone = "IST")
    public void pincodesReconciliationJob() {
        this.reconcilePincodesFromLambda(userRequestManager.fetchAllUserRequests());
    }

    public void reconcilePincodesFromLambda(List<UserRequest> userRequests) {
        log.info("Starting reconciliation of missing pincodes from AWS Lambda");
        reconciliationStats.reset();
        reconciliationStats.noteStartTime();

        userRequests.stream()
                .flatMap(userRequest -> userRequest.getPincodes().stream())
                .distinct()
                .peek(pincode -> log.debug("reconciliating pincode {}", pincode))
                .filter(missingPincodesInStore())
                .peek(pincode -> reconciliationStats.incrementUnknownPincodes())
                .flatMap(cowinLambdaWrapper::fetchSessionsByPincode)
                .flatMap(Optional::stream)
                .peek(logFailedReconciliations())
                .filter(centersWithData())
                .map(this::fetchDistrictsFromStore)
                .filter(Objects::nonNull)
                .forEach(persistPincode());

        reconciliationStats.noteEndTime();
        log.info("[PINCODE RECONCILIATION] Unknown: {}, Failed: {}, Missing district: {}, Successful: {}, Time taken: {}",
                reconciliationStats.unknownPincodes(), reconciliationStats.failedReconciliations(),
                reconciliationStats.failedWithUnknownDistrict(), reconciliationStats.successfulReconciliations(), reconciliationStats.timeTaken());
        botService.notifyOwner(String.format("[PINCODE RECONCILIATION] Unknown: %d, Failed: %d, Missing district: %d, Successful: %d, Time taken: %s",
                reconciliationStats.unknownPincodes(), reconciliationStats.failedReconciliations(),
                reconciliationStats.failedWithUnknownDistrict(), reconciliationStats.successfulReconciliations(), reconciliationStats.timeTaken()));
    }

    @NotNull
    private Predicate<String> missingPincodesInStore() {
        return pincode -> !metadataStore.pincodeExists(pincode);
    }

    @NotNull
    private Consumer<ImmutablePair<String, District>> persistPincode() {
        return pair -> {
            this.metadataStore.persistPincode(Pincode.builder()
                    .pincode(pair.getLeft())
                    .district(pair.getRight())
                    .build());
            log.info("Reconciliation successful for pincode {}", pair.getLeft());
            reconciliationStats.incrementSuccessfulReconciliations();
        };
    }

    @Nullable
    private ImmutablePair<String, District> fetchDistrictsFromStore(VaccineCenters vaccineCenters) {
        String districtName = vaccineCenters.getCenters().get(0).getDistrictName();
        String stateName = vaccineCenters.getCenters().get(0).getStateName();
        String pincode = String.valueOf(vaccineCenters.getCenters().get(0).getPincode());
        District district = metadataStore.fetchDistrictByNameAndState(districtName, stateName);
        if (isNull(district)) {
            log.warn("No district found in DB for name {}", districtName);
            reconciliationStats.incrementFailedWithUnknownDistrict();
            return null;
        }
        return new ImmutablePair<>(pincode, district);
    }

    @NotNull
    private Predicate<VaccineCenters> centersWithData() {
        return vaccineCenters -> nonNull(vaccineCenters) && !vaccineCenters.getCenters().isEmpty();
    }

    @NotNull
    private Consumer<VaccineCenters> logFailedReconciliations() {
        return vaccineCenters -> {
            if (isNull(vaccineCenters) || vaccineCenters.getCenters().isEmpty()) {
                reconciliationStats.incrementFailedReconciliations();
            }
        };
    }

}
