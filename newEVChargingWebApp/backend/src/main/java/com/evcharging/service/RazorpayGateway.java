package com.evcharging.service;

import com.evcharging.exception.PaymentException;
import com.evcharging.exception.PaymentGatewayException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class RazorpayGateway {

    /**
     * What Razorpay itself reports about a payment id.
     */
    public record GatewayPayment(String orderId, long amountPaise, String status) {
        public boolean isPaid() {
            return "captured".equalsIgnoreCase(status) || "authorized".equalsIgnoreCase(status);
        }

        public boolean isFailed() {
            return "failed".equalsIgnoreCase(status);
        }
    }


    private final RazorpayClient client;      // null when keys are not configured
    private final String keyId;
    private final String keySecret;

    public RazorpayGateway(ObjectProvider<RazorpayClient> clientProvider, @Value("${razorpay.key.id:}") String keyId, @Value("${razorpay.key.secret:}") String keySecret) {
        this.client = clientProvider.getIfAvailable();
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public boolean isConfigured() {
        return client != null;
    }

    /**
     * The PUBLIC key id (safe for the browser). The secret never leaves the server.
     */
    public String keyId() {
        return keyId;
    }

    private RazorpayClient requireClient() {
        if (client == null) {
            throw new PaymentException("Online payments are not configured on the server.");
        }
        return client;
    }

    public String createOrder(long amountPaise, String receipt) {
        try {
            JSONObject request = new JSONObject();
            request.put("amount", amountPaise);
            request.put("currency", "INR");
            request.put("receipt", receipt);
            Order order = requireClient().orders.create(request);
            return order.get("id");
        } catch (RazorpayException e) {
            throw new PaymentGatewayException("Unable to create Razorpay order.", e);
        }
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        requireClient();
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);
            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            return false;
        }
    }

    /**
     * Asks Razorpay for the real order id / amount / status of a payment id (never trust the browser's word).
     */
    public GatewayPayment fetchPayment(String paymentId) {
        try {
            com.razorpay.Payment payment = requireClient().payments.fetch(paymentId);
            String orderId = payment.get("order_id");
            Number amount = payment.get("amount");
            String status = payment.get("status");
            return new GatewayPayment(orderId, amount.longValue(), status);
        } catch (RazorpayException e) {
            throw new PaymentGatewayException("Unable to confirm the payment with Razorpay.", e);
        }
    }
}
