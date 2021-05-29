package org.covid19.vaccinetracker.availability.cowin;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.covid19.vaccinetracker.availability.model.ConfirmOtpResponse;
import org.covid19.vaccinetracker.availability.model.GenerateOtpResponse;
import org.covid19.vaccinetracker.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class CowinApiAuth {
    private final AtomicReference<String> bearerToken = new AtomicReference<>("");
    private String transactionId = "";
    private final AtomicBoolean awaitingOtp = new AtomicBoolean(false);

    private final CowinApiOtpClient cowinApiOtpClient;
    private final CowinConfig cowinConfig;

    public CowinApiAuth(CowinApiOtpClient cowinApiOtpClient, CowinConfig cowinConfig) {
        this.cowinApiOtpClient = cowinApiOtpClient;
        this.cowinConfig = cowinConfig;
    }

    @Scheduled(cron = "${jobs.cron.cowin.api.auth:-}", zone = "IST")
    public void refreshCowinToken() {
        log.info("Refreshing Cowin Auth Token");
        if (this.awaitingOtp.get()) {
            log.warn("Still awaiting OTP. Resetting and canceling current run.");
            reset();
            return;
        }
        final GenerateOtpResponse generateOtpResponse = this.cowinApiOtpClient.generateOtp(cowinConfig.getAuthMobile());
        if (isNull(generateOtpResponse)) {
            return;
        }
        this.transactionId = generateOtpResponse.getTransactionId();
        this.awaitingOtp.compareAndSet(false, true);
    }

    public void handleOtpCallback(String callBackMessage) {
        if (!this.awaitingOtp.get()) {
            log.warn("Received OTP callback but not awaiting OTP at this time, skipping. Callback: {}", callBackMessage);
            return;
        }
        String otpHash = parseOtp(callBackMessage);
        if (isNull(otpHash)) {
            reset();
            return;
        }
        final ConfirmOtpResponse confirmOtpResponse = this.cowinApiOtpClient.confirmOtp(this.transactionId, otpHash);
        if (isNull(confirmOtpResponse)) {
            reset();
            return;
        }
        this.bearerToken.set(confirmOtpResponse.getToken());
        this.awaitingOtp.set(false);
        this.transactionId = "";
        log.info("CoWIN authentication completed.");
    }

    void reset() {
        this.awaitingOtp.set(false);
        this.transactionId = "";
        this.bearerToken.set("");
    }

    public String getBearerToken() {
        return bearerToken.get();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public boolean isAwaitingOtp() {
        return awaitingOtp.get();
    }

    public boolean isAvailable() {
        return Utils.isValidJwtToken(this.bearerToken.get());
    }

    private String parseOtp(String message) {
        if (isInvalidCallbackMessage(message)) {
            log.warn("Found invalid/unexpected callback message: {}", message);
            return null;
        }
        return DigestUtils.sha256Hex(extractOtp(message));
    }

    private String extractOtp(String message) {
        return message.substring(37, 43);
    }

    private boolean isInvalidCallbackMessage(String message) {
        return isNull(message) || message.isEmpty()
                || !message.startsWith("Your OTP to register/access CoWIN is ")
                || message.length() < 43
                || !NumberUtils.isParsable(message.substring(37, 43));
    }
}
