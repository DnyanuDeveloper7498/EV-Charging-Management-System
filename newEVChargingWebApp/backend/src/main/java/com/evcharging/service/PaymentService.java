package com.evcharging.service;

import com.evcharging.dto.VerifyPaymentRequest;

import com.evcharging.exception.PaymentException;
import com.evcharging.model.ChargingSession;
import com.evcharging.model.Payment;
import com.evcharging.model.PaymentStatus;
import com.evcharging.model.WalletTransaction;
import com.evcharging.repository.ChargingSessionRepository;

import com.evcharging.repository.PaymentRepository;
import com.evcharging.repository.WalletTransactionRepository;
import com.evcharging.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ONE payment lifecycle for a charging session, whichever way the customer pays:
 * <pre>
 *   session stopped  -> payment row PENDING (amount copied from the session, never from the client)
 *   pay with wallet  -> wallet debited + ledger entry     -> SUCCESS (method WALLET)
 *   pay with Razorpay-> order created (amount from DB)    -> signature + gateway check -> SUCCESS (method RAZORPAY)
 *   payment failed   -> FAILED (persisted, customer may retry -> PENDING again)
 * </pre>
 * There is exactly one payment row per session (unique index) and every transition happens while holding
 * that row's lock, so double clicks, retries and the webhook can never pay a session twice.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public static final String METHOD_PENDING = "PENDING";
    public static final String METHOD_WALLET = "WALLET";
    public static final String METHOD_RAZORPAY = "RAZORPAY";
    public static final String METHOD_FREE = "NONE";

    private static final BigDecimal MIN_ONLINE_AMOUNT = new BigDecimal("1.00");

    private final PaymentRepository paymentRepository;
    private final WalletTransactionRepository walletTxnRepository;
    private final WalletService walletService;
    private final RazorpayGateway gateway;
    private final AccessService access;
    private final ObjectMapper objectMapper;

    private final InAppNotificationService inAppNotificationService;

    private final ChargingSessionRepository chargingSessionRepository;

    private final Clock clock;
    private final String webhookSecret;

    public PaymentService(PaymentRepository paymentRepository, WalletTransactionRepository walletTxnRepository, WalletService walletService, RazorpayGateway gateway, AccessService access, ObjectMapper objectMapper, InAppNotificationService inAppNotificationService, ChargingSessionRepository chargingStationRepository, Clock clock, @Value("${razorpay.webhook.secret:}") String webhookSecret) {
        this.paymentRepository = paymentRepository;
        this.walletTxnRepository = walletTxnRepository;
        this.walletService = walletService;
        this.gateway = gateway;
        this.access = access;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.inAppNotificationService = inAppNotificationService;
        this.chargingSessionRepository = chargingStationRepository;
        this.webhookSecret = webhookSecret;
    }

    // ------------------------------------------------------------------ reads

    /**
     * The caller's own payments only.
     */
    public List<Payment> getMyPayments() {
        return paymentRepository.findByUserId(SecurityUtils.currentUserId());
    }

    public List<Payment> getAllPayments() {
        SecurityUtils.requireAdminOrFinanceAdmin();
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(int paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new PaymentException("Payment not found."));
        access.requirePaymentVisible(payment);
        return payment;
    }

    public Payment getPaymentBySessionId(int sessionId) {
        access.sessionForCurrentUserOrAdmin(sessionId);
        return paymentRepository.findBySessionId(sessionId).orElseThrow(() -> new PaymentException("Payment not found."));
    }

    public List<Payment> getPaymentsByStatus(String paymentStatus) {
        SecurityUtils.requireAdminOrFinanceAdmin();
        return paymentRepository.findByPaymentStatus(paymentStatus.toUpperCase());
    }


    // ------------------------------------------------- create the payment row

    /**
     * Called when a session stops. The amount is always the session's own total_cost.
     */
    @Transactional
    public Payment createPendingPayment(ChargingSession session) {
        return paymentRepository.findBySessionId(session.getsessionId()).orElseGet(() -> {
            Payment payment = new Payment();
            payment.setsessionId(session.getsessionId());
            payment.setAmount(session.gettotalCost());
            payment.setpaymentTime(LocalDateTime.now(clock));

            if (session.gettotalCost().signum() == 0) {
                payment.setpaymentStatus(PaymentStatus.SUCCESS);   // nothing to pay
                payment.setpaymentMethod(METHOD_FREE);
            } else {
                payment.setpaymentStatus(PaymentStatus.PENDING);
                payment.setpaymentMethod(METHOD_PENDING);
            }
            return paymentRepository.save(payment);
        });
    }

    /**
     * Legacy endpoint: makes sure the caller's completed session has its payment row.
     */
    @Transactional
    public Payment createPayment(int sessionId, String paymentMethod) {
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase();
        if (!METHOD_WALLET.equals(method) && !METHOD_RAZORPAY.equals(method)) {
            throw new IllegalArgumentException("Payment method must be WALLET or RAZORPAY.");
        }
        return ensurePayment(sessionId);
    }

    /**
     * Finds (or lazily creates, for sessions stopped before this version) the locked payment row of a session.
     */
    private Payment ensurePayment(int sessionId) {
        ChargingSession session = access.sessionOwnedByCurrentUser(sessionId);

        if (session.getendTime() == null) {
            throw new IllegalArgumentException("Charging session must be completed before payment.");
        }

        createPendingPayment(session);
        return paymentRepository.findBySessionIdForUpdate(sessionId).orElseThrow(() -> new PaymentException("Payment record not found."));
    }

    // ------------------------------------------------------------ wallet pay

    @Transactional
    public Map<String, Object> payWithWallet(int sessionId) {
        Payment payment = ensurePayment(sessionId);

        if (PaymentStatus.SUCCESS.equals(payment.getpaymentStatus())) {
            throw new IllegalStateException("This session is already paid.");
        }

        BigDecimal newBalance = walletService.debitForSession(SecurityUtils.currentUserId(), payment.getAmount(), sessionId);

        payment.setpaymentStatus(PaymentStatus.SUCCESS);
        payment.setpaymentMethod(METHOD_WALLET);
        payment.setpaymentTime(LocalDateTime.now(clock));
        payment.setFailureReason(null);


        paymentRepository.save(payment);

        notifyPaymentSuccess(payment);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Payment successful");
        response.put("paymentId", payment.getpaymentId());
        response.put("sessionId", payment.getsessionId());
        response.put("paymentStatus", payment.getpaymentStatus());
        response.put("walletBalance", newBalance);
        return response;
    }

    // --------------------------------------------------------- razorpay pay

    @Transactional
    public Map<String, Object> createRazorpayOrder(int sessionId) {
        Payment payment = ensurePayment(sessionId);

        if (PaymentStatus.SUCCESS.equals(payment.getpaymentStatus())) {
            throw new IllegalStateException("This session is already paid.");
        }

        BigDecimal amount = payment.getAmount();   // the amount stored for the session, never a client value
        if (amount.compareTo(MIN_ONLINE_AMOUNT) < 0) {
            throw new IllegalArgumentException("Amounts below ₹1.00 cannot be paid online. Please pay from your wallet.");
        }
        long amountPaise = WalletService.toPaise(amount);

        // Reuse the open order for a PENDING payment; a FAILED attempt gets a brand-new order.
        boolean reuse = payment.getRazorpayOrderId() != null && PaymentStatus.PENDING.equals(payment.getpaymentStatus());

        if (!reuse) {
            String orderId = gateway.createOrder(amountPaise, "ev_session_" + sessionId);
            payment.setRazorpayOrderId(orderId);
            payment.setRazorpayPaymentId(null);
            payment.setpaymentStatus(PaymentStatus.PENDING);
            payment.setpaymentMethod(METHOD_RAZORPAY);
            payment.setFailureReason(null);
            paymentRepository.save(payment);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("keyId", gateway.keyId());
        response.put("orderId", payment.getRazorpayOrderId());
        response.put("amount", amountPaise);
        response.put("currency", "INR");
        response.put("paymentRecordId", payment.getpaymentId());
        response.put("sessionId", sessionId);
        return response;
    }

    @Transactional
    public Payment verifyPayment(VerifyPaymentRequest request) {
        int sessionId = request.sessionId();
        Payment payment = ensurePayment(sessionId);
        String razorpayPaymentId = request.razorpayPaymentId();

        if (PaymentStatus.SUCCESS.equals(payment.getpaymentStatus())) {
            if (razorpayPaymentId.equals(payment.getRazorpayPaymentId())) {
                return payment;                       // idempotent retry of the same confirmation
            }
            throw new IllegalStateException("Payment already completed.");
        }

        String orderId = payment.getRazorpayOrderId();
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("No online payment was started for this session.");
        }

        if (!gateway.verifySignature(orderId, razorpayPaymentId, request.razorpaySignature())) {
            throw new PaymentException("Payment signature verification failed.");
        }

        // The signature proves the order id + payment id belong together. Ask Razorpay what was really paid.
        RazorpayGateway.GatewayPayment gatewayPayment = gateway.fetchPayment(razorpayPaymentId);
        if (!orderId.equals(gatewayPayment.orderId()) || gatewayPayment.amountPaise() != WalletService.toPaise(payment.getAmount()) || !gatewayPayment.isPaid()) {
            throw new PaymentException("Payment details do not match this session's bill.");
        }

        return markRazorpaySuccess(payment, razorpayPaymentId);
    }

    /**
     * The browser reports that the checkout failed/was abandoned. Persist it so the bill shows FAILED and can be retried.
     */
    @Transactional
    public Payment markFailed(int sessionId, String reason) {
        Payment payment = ensurePayment(sessionId);


        if (!PaymentStatus.SUCCESS.equals(payment.getpaymentStatus()) &&
            !PaymentStatus.FAILED.equals(payment.getpaymentStatus()))
        {

            payment.setpaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason(truncate(reason==null || reason.isBlank()?"Payment failed.": reason));
            paymentRepository.save(payment);

            notifyPaymentFailure(payment);
        }


        return payment;
    }

    private Payment markRazorpaySuccess(Payment payment, String razorpayPaymentId) {
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setpaymentStatus(PaymentStatus.SUCCESS);
        payment.setpaymentMethod(METHOD_RAZORPAY);
        payment.setpaymentTime(LocalDateTime.now(clock));
        payment.setFailureReason(null);

        Payment saved=paymentRepository.save(payment);

        notifyPaymentSuccess(saved);

        return saved;
    }

    // --------------------------------------------------------------- webhook

    /**
     * Razorpay server-to-server notification (the safety net for a customer who closes the browser right after
     * paying). The raw body is authenticated with HMAC-SHA256 using the webhook secret.
     */
    @Transactional
    public void handleWebhook(String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new PaymentException("Webhook is not configured.");
        }
        if (signature == null || !signatureMatches(rawBody, signature)) {
            throw new PaymentException("Invalid webhook signature.");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid webhook payload.");
        }

        String event = root.path("event").asText("");
        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        String orderId = paymentEntity.path("order_id").asText("");
        String razorpayPaymentId = paymentEntity.path("id").asText("");
        long amountPaise = paymentEntity.path("amount").asLong(-1);

        if (orderId.isEmpty() || razorpayPaymentId.isEmpty()) {
            return; // an event we do not care about
        }

        switch (event) {
            case "payment.captured", "order.paid" -> onGatewayPaid(orderId, razorpayPaymentId, amountPaise);
            case "payment.failed" -> onGatewayFailed(orderId, paymentEntity.path("error_description").asText(""));
            default -> log.debug("Ignoring Razorpay event {}", event);
        }
    }

    private void onGatewayPaid(String orderId, String razorpayPaymentId, long amountPaise) {
        Payment payment = paymentRepository.findByRazorpayOrderIdForUpdate(orderId).orElse(null);

        if (payment != null) {
            if (PaymentStatus.SUCCESS.equals(payment.getpaymentStatus())) {
                if (!razorpayPaymentId.equals(payment.getRazorpayPaymentId())) {
                    // e.g. the bill was already paid from the wallet while an online order was still open
                    log.warn("DUPLICATE PAYMENT: session {} is already paid ({}) but Razorpay payment {} was captured. " + "A manual refund is required.", payment.getsessionId(), payment.getpaymentMethod(), razorpayPaymentId);
                }
                return;
            }
            if (WalletService.toPaise(payment.getAmount()) != amountPaise) {
                log.error("Webhook amount {} does not match bill for session {}", amountPaise, payment.getsessionId());
                return;
            }
            markRazorpaySuccess(payment, razorpayPaymentId);
            return;
        }

        WalletTransaction txn = walletTxnRepository.findByRazorpayOrderIdForUpdate(orderId).orElse(null);
        if (txn != null) {
            if (WalletService.toPaise(txn.getAmount()) != amountPaise) {
                log.error("Webhook amount {} does not match top-up {}", amountPaise, txn.getTxnId());
                return;
            }
            walletService.completeTopup(txn, razorpayPaymentId);
        }
    }

    private void onGatewayFailed(String orderId, String description) {
        Payment payment = paymentRepository.findByRazorpayOrderIdForUpdate(orderId).orElse(null);

        if (payment != null) {
            if (PaymentStatus.PENDING.equals(payment.getpaymentStatus())) {
                payment.setpaymentStatus(PaymentStatus.FAILED);
                payment.setFailureReason(truncate(description.isBlank() ? "Payment failed." : description));
                paymentRepository.save(payment);

                notifyPaymentFailure(payment);
            }
            return;
        }

        walletTxnRepository.findByRazorpayOrderIdForUpdate(orderId).ifPresent(walletService::failTopup);
    }

    private boolean signatureMatches(String body, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }


    private void notifyPaymentSuccess(Payment payment) {
        notifyPayment(payment, "Payment Successful", "Payment of ₹" + payment.getAmount() + " for session #" + payment.getsessionId() + " was successful (" + payment.getpaymentMethod() + ").", "PAYMENT_SUCCESS");
    }

    private void notifyPaymentFailure(Payment payment) {
        String reason = payment.getFailureReason() == null ? "Payment failed." : payment.getFailureReason();
        notifyPayment(payment, "Payment Failed", "Payment of ₹" + payment.getAmount() + " for session #" + payment.getsessionId() + " failed. Reason: " + reason + " You can retry from the Billing page.", "PAYMENT_FAILED");
    }


    private void notifyPayment(Payment payment, String title, String message, String type) {
        try {
            ChargingSession session = chargingSessionRepository.findById(payment.getsessionId()).orElse(null);
            if (session == null) {
                return;
            }

            inAppNotificationService.createNotificationIsolated(access.ownerOfSession(session), session.getbookingId(), title, message, type);
        } catch (Exception e) {

            log.warn("Could not create payment notification for session {}", payment.getsessionId(), e);
        }
    }


    private static String truncate(String value) {
        return value.length() > 255 ? value.substring(0, 255) : value;
    }


}
