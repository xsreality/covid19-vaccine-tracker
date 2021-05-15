package org.covid19.vaccinetracker.cowin;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.covid19.vaccinetracker.model.ConfirmOtpResponse;
import org.covid19.vaccinetracker.model.GenerateOtpResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class CowinApiAuth {
    private final AtomicReference<String> bearerToken = new AtomicReference<>("");
    private final AtomicReference<String> transactionId = new AtomicReference<>("");
    private final AtomicBoolean awaitingOtp = new AtomicBoolean(false);

    private final CowinApiOtpClient cowinApiOtpClient;
    private final CowinConfig cowinConfig;

    public CowinApiAuth(CowinApiOtpClient cowinApiOtpClient, CowinConfig cowinConfig) {
        this.cowinApiOtpClient = cowinApiOtpClient;
        this.cowinConfig = cowinConfig;
    }

    public void refreshCowinToken() {
        log.info("Refreshing Cowin Auth Token");
        if (this.awaitingOtp.get()) {
            log.info("Still awaiting OTP. Resetting and canceling current run.");
            this.awaitingOtp.set(false);
            return;
        }
        final GenerateOtpResponse generateOtpResponse = this.cowinApiOtpClient.generateOtp(cowinConfig.getAuthMobile());
        if (isNull(generateOtpResponse)) {
            return;
        }
        this.transactionId.set(generateOtpResponse.getTransactionId());
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
        final ConfirmOtpResponse confirmOtpResponse = this.cowinApiOtpClient.confirmOtp(this.transactionId.get(), otpHash);
        if (isNull(confirmOtpResponse)) {
            reset();
            return;
        }
        this.bearerToken.set(confirmOtpResponse.getToken());
        reset();
    }

    void reset() {
        this.awaitingOtp.set(false);
        this.transactionId.set("");
    }

    public String getBearerToken() {
        return bearerToken.get();
    }

    public String getTransactionId() {
        return transactionId.get();
    }

    public boolean isAwaitingOtp() {
        return awaitingOtp.get();
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
