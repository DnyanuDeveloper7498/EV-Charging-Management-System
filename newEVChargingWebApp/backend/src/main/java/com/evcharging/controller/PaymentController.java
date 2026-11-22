package com.evcharging.controller;

import com.evcharging.dto.CreatePaymentRequest;
import com.evcharging.dto.PaymentFailedRequest;
import com.evcharging.dto.VerifyPaymentRequest;
import com.evcharging.model.Payment;
import com.evcharging.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** The CALLER's own payments (previously this returned every customer's payments). */
    @GetMapping
    public List<Payment> getMyPayments() {
        return paymentService.getMyPayments();
    }

    //---------- ADMIN only-----------


    @GetMapping("/all")
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{paymentId}")
    public Payment getPaymentById(@PathVariable int paymentId) {
        return paymentService.getPaymentById(paymentId);
    }

    @GetMapping("/session/{sessionId}")
    public Payment getPaymentBySessionId(@PathVariable int sessionId) {
        return paymentService.getPaymentBySessionId(sessionId);
    }

    //---------- ADMIN only-----------


    @GetMapping("/status/{paymentStatus}")
    public List<Payment> getPaymentsByStatus(@PathVariable String paymentStatus) {
        return paymentService.getPaymentsByStatus(paymentStatus);
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request.sessionId(), request.paymentMethod());
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @PostMapping("/pay-with-wallet")
    public Map<String, Object> payWithWallet(@RequestParam int sessionId) {
        return paymentService.payWithWallet(sessionId);
    }

    @PostMapping("/create-order")
    public Map<String, Object> createRazorpayOrder(@RequestParam int sessionId) {
        return paymentService.createRazorpayOrder(sessionId);
    }

    @PostMapping("/verify-payment")
    public Map<String, Object> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        Payment payment = paymentService.verifyPayment(request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Payment verified successfully");
        response.put("paymentId", payment.getpaymentId());
        response.put("sessionId", payment.getsessionId());
        response.put("paymentStatus", payment.getpaymentStatus());
        return response;
    }

    @PostMapping("/failed")
    public Map<String, Object> paymentFailed(@Valid @RequestBody PaymentFailedRequest request) {
        Payment payment = paymentService.markFailed(request.sessionId(), request.reason());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paymentId", payment.getpaymentId());
        response.put("sessionId", payment.getsessionId());
        response.put("paymentStatus", payment.getpaymentStatus());
        return response;
    }

    /**
     * Razorpay webhook (public URL, authenticated by the X-Razorpay-Signature header).
     * Configure it in the Razorpay dashboard: POST https://<your-api>/api/payments/webhook
     * with events payment.captured, order.paid and payment.failed.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestBody String payload,
                                                       @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
