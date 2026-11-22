package com.evcharging.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Razorpay client exists only when both keys are configured, so the rest of the application
 * (login, bookings, wallet demo mode ...) still starts without payment keys. Keys are never logged.
 */
@Configuration
public class RazorpayConfig {

    @Bean
    @ConditionalOnExpression("!'${razorpay.key.id:}'.isEmpty() && !'${razorpay.key.secret:}'.isEmpty()")
    public RazorpayClient razorpayClient(@Value("${razorpay.key.id}") String keyId,
                                         @Value("${razorpay.key.secret}") String keySecret) throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }
}
