package org.covid19.vaccinetracker.availability.cowin;

import org.covid19.vaccinetracker.availability.model.ConfirmOtpResponse;
import org.covid19.vaccinetracker.availability.model.GenerateOtpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CowinApiAuthTest {
    @Mock
    private CowinApiOtpClient cowinApiOtpClient;
    private final CowinConfig cowinConfig = new CowinConfig();
    private CowinApiAuth cowinApiAuth;

    @BeforeEach
    public void setup() {
        cowinConfig.setAuthMobile("9999999999");
        cowinApiAuth = new CowinApiAuth(cowinApiOtpClient, cowinConfig);
    }

    @Test
    public void generateAndValidateOtp() {
        GenerateOtpResponse generateOtpResponse = GenerateOtpResponse.builder().transactionId("xxx").build();
        when(cowinApiOtpClient.generateOtp(cowinConfig.getAuthMobile())).thenReturn(generateOtpResponse);
        cowinApiAuth.refreshCowinToken();
        assertThat(cowinApiAuth.getTransactionId(), is(equalTo("xxx")));
        assertTrue(cowinApiAuth.isAwaitingOtp());

        ConfirmOtpResponse confirmOtpResponse = ConfirmOtpResponse.builder().token("xyz-token").build();
        when(cowinApiOtpClient.confirmOtp(generateOtpResponse.getTransactionId(),
                "1544344b7ad152d165ceadbb7e76459757d5143d77b481fdede03d032acbf481")).thenReturn(confirmOtpResponse);
        String callback = "Your OTP to register/access CoWIN is 181895. It will be valid for 3 minutes. - CoWIN";
        cowinApiAuth.handleOtpCallback(callback);
        assertThat(cowinApiAuth.getBearerToken(), is(equalTo("xyz-token")));
        // txnId should be reset, isAwaitingOtp should be false
        assertThat(cowinApiAuth.getTransactionId(), is(emptyString()));
        assertFalse(cowinApiAuth.isAwaitingOtp());
    }

    @Test
    public void testHandlingFailedGenerationOfOtp() {
        when(cowinApiOtpClient.generateOtp(cowinConfig.getAuthMobile())).thenReturn(null);
        cowinApiAuth.refreshCowinToken();
        assertThat(cowinApiAuth.getTransactionId(), is(emptyString()));
        assertFalse(cowinApiAuth.isAwaitingOtp());

        String callback = "Your OTP to register/access CoWIN is 181895. It will be valid for 3 minutes. - CoWIN";
        cowinApiAuth.handleOtpCallback(callback);
        assertThat(cowinApiAuth.getBearerToken(), is(emptyString()));
        assertFalse(cowinApiAuth.isAwaitingOtp());
    }

    @Test
    public void testHandlingGenerationOfOtpAndFailedConfirmation() {
        // generate OTP goes fine...
        GenerateOtpResponse generateOtpResponse = GenerateOtpResponse.builder().transactionId("xxx").build();
        when(cowinApiOtpClient.generateOtp(cowinConfig.getAuthMobile())).thenReturn(generateOtpResponse);
        cowinApiAuth.refreshCowinToken();
        assertThat(cowinApiAuth.getTransactionId(), is(equalTo("xxx")));
        assertTrue(cowinApiAuth.isAwaitingOtp());

        // ...confirm OTP fails for any reason...
        when(cowinApiOtpClient.confirmOtp(generateOtpResponse.getTransactionId(),
                "1544344b7ad152d165ceadbb7e76459757d5143d77b481fdede03d032acbf481")).thenReturn(null);
        String callback = "Your OTP to register/access CoWIN is 181895. It will be valid for 3 minutes. - CoWIN";
        cowinApiAuth.handleOtpCallback(callback);

        // ...variables should be reset again
        assertThat(cowinApiAuth.getTransactionId(), is(emptyString()));
        assertThat(cowinApiAuth.getBearerToken(), is(emptyString()));
        assertFalse(cowinApiAuth.isAwaitingOtp());
    }

    @Test
    public void testInvalidCallbackMessage() {
        // generate OTP goes fine...
        GenerateOtpResponse generateOtpResponse = GenerateOtpResponse.builder().transactionId("xxx").build();
        when(cowinApiOtpClient.generateOtp(cowinConfig.getAuthMobile())).thenReturn(generateOtpResponse);
        cowinApiAuth.refreshCowinToken();
        assertThat(cowinApiAuth.getTransactionId(), is(equalTo("xxx")));
        assertTrue(cowinApiAuth.isAwaitingOtp());

        // ...we receive invalid callback message...
        String invalidCallback = "This is an invalid callback message without any OTP.";
        cowinApiAuth.handleOtpCallback(invalidCallback);

        // ...variables should be reset again
        assertThat(cowinApiAuth.getTransactionId(), is(emptyString()));
        assertThat(cowinApiAuth.getBearerToken(), is(emptyString()));
        assertFalse(cowinApiAuth.isAwaitingOtp());
    }

    @Test
    public void testConsecutiveRefreshShouldNotTriggerMultipleOtpCalls() {
        // generate OTP goes fine...
        GenerateOtpResponse generateOtpResponse = GenerateOtpResponse.builder().transactionId("xxx").build();
        when(cowinApiOtpClient.generateOtp(cowinConfig.getAuthMobile())).thenReturn(generateOtpResponse);
        cowinApiAuth.refreshCowinToken();
        assertThat(cowinApiAuth.getTransactionId(), is(equalTo("xxx")));
        assertTrue(cowinApiAuth.isAwaitingOtp());

        // ...try to generate otp again before it can be confirmed...
        cowinApiAuth.refreshCowinToken();

        // ...should not trigger another call
        verify(cowinApiOtpClient, times(1)).generateOtp(anyString());
        assertFalse(cowinApiAuth.isAwaitingOtp());
    }
}
