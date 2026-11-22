package com.evcharging.service;

import com.evcharging.dto.TopupFailureRequest;
import com.evcharging.dto.TopupVerifyRequest;
import com.evcharging.dto.WalletResponse;
import com.evcharging.exception.PaymentException;
import com.evcharging.exception.UserNotFoundException;
import com.evcharging.exception.WalletException;
import com.evcharging.model.PaymentStatus;
import com.evcharging.model.User;
import com.evcharging.model.WalletTransaction;
import com.evcharging.repository.UserRepository;
import com.evcharging.repository.WalletTransactionRepository;
import com.evcharging.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WalletService {

    private final UserRepository userRepository;
    private final WalletTransactionRepository txnRepository;
    private final RazorpayGateway gateway;
    private final Clock clock;

    private final boolean demoTopupEnabled;
    private final BigDecimal minTopup;
    private final BigDecimal maxTopup;

    private final InAppNotificationService inAppNotificationService;


    public WalletService(UserRepository userRepository, WalletTransactionRepository txnRepository, RazorpayGateway gateway, Clock clock, InAppNotificationService inAppNotificationService, @Value("${app.wallet.demo-topup-enabled}") boolean demoTopupEnabled, @Value("${app.wallet.min-topup}") BigDecimal minTopup, @Value("${app.wallet.max-topup}") BigDecimal maxTopup) {
        this.userRepository = userRepository;
        this.txnRepository = txnRepository;
        this.gateway = gateway;
        this.clock = clock;
        this.inAppNotificationService = inAppNotificationService;
        this.demoTopupEnabled = demoTopupEnabled;
        this.minTopup = minTopup;
        this.maxTopup = maxTopup;
    }

    // ------------------------------------------------------------------ reads

    public WalletResponse getWallet() {
        User user = userRepository.findById(SecurityUtils.currentUserId()).orElseThrow(() -> new UserNotFoundException("User not found."));
        return WalletResponse.of(user, demoTopupEnabled);
    }

    public List<WalletTransaction> getTransactions() {
        return txnRepository.findByUserIdOrderByCreatedAtDesc(SecurityUtils.currentUserId());
    }

    // ------------------------------------------------------- top-up (dev only)


    @Transactional
    public WalletResponse demoTopup(BigDecimal amount) {
        if (!demoTopupEnabled) {
            throw new AccessDeniedException("Direct wallet top-up is disabled. Please add money through Razorpay.");
        }
        BigDecimal value = validateTopupAmount(amount);
        User user = lockCurrentUser();

        BigDecimal newBalance = user.getWalletBalance().add(value);
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        txnRepository.save(newTxn(user.getUserId(), WalletTransaction.CREDIT, value, newBalance, PaymentStatus.SUCCESS, WalletTransaction.REF_TOPUP_DEMO, null, null, null));

        inAppNotificationService.createNotification(user.getUserId(), null, "Wallet Top-up Successful",
                "₹" + value + " has been added to your wallet. New balance: ₹" + newBalance,
                "WALLET_TOPUP_SUCCESS");

        return WalletResponse.of(user, demoTopupEnabled);
    }

    // ----------------------------------------------- top-up through Razorpay

    @Transactional
    public Map<String, Object> createTopupOrder(BigDecimal amount) {
        BigDecimal value = validateTopupAmount(amount);
        int userId = SecurityUtils.currentUserId();
        long amountPaise = toPaise(value);

        String orderId = gateway.createOrder(amountPaise, "ev_topup_" + userId + "_" + System.currentTimeMillis());

        WalletTransaction txn = txnRepository.save(newTxn(userId, WalletTransaction.CREDIT, value, null, PaymentStatus.PENDING, WalletTransaction.REF_TOPUP, null, orderId, null));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("keyId", gateway.keyId());
        response.put("orderId", orderId);
        response.put("amount", amountPaise);
        response.put("currency", "INR");
        response.put("transactionId", txn.getTxnId());
        return response;
    }

    @Transactional
    public WalletResponse verifyTopup(TopupVerifyRequest request) {
        WalletTransaction txn = txnRepository.findByRazorpayOrderIdForUpdate(request.razorpayOrderId()).orElseThrow(() -> new PaymentException("Top-up order not found."));

        if (txn.getUserId() != SecurityUtils.currentUserId()) {
            throw new PaymentException("Top-up order not found.");
        }

        if (PaymentStatus.SUCCESS.equals(txn.getStatus())) {
            if (request.razorpayPaymentId().equals(txn.getRazorpayPaymentId())) {
                return getWallet(); // idempotent retry
            }
            throw new PaymentException("This top-up has already been completed.");
        }

        if (!gateway.verifySignature(txn.getRazorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature())) {
            throw new PaymentException("Payment signature verification failed.");
        }

        // confirm with Razorpay that the money really arrived, for the amount we asked for
        RazorpayGateway.GatewayPayment gatewayPayment = gateway.fetchPayment(request.razorpayPaymentId());
        if (!txn.getRazorpayOrderId().equals(gatewayPayment.orderId()) || gatewayPayment.amountPaise() != toPaise(txn.getAmount()) || !gatewayPayment.isPaid()) {
            throw new PaymentException("Payment details do not match this top-up.");
        }

        completeTopup(txn, request.razorpayPaymentId());
        return getWallet();
    }

    /**
     * Credits a PENDING top-up exactly once. Used by the browser verification above and by the Razorpay webhook,
     * whichever arrives first. The caller must already hold the transaction row lock.
     */
    @Transactional
    public void completeTopup(WalletTransaction txn, String razorpayPaymentId) {
        if (PaymentStatus.SUCCESS.equals(txn.getStatus())) {
            return;
        }
        User user = userRepository.findByIdForUpdate(txn.getUserId()).orElseThrow(() -> new UserNotFoundException("User not found."));

        BigDecimal newBalance = user.getWalletBalance().add(txn.getAmount());
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        txn.setStatus(PaymentStatus.SUCCESS);
        txn.setBalanceAfter(newBalance);
        txn.setRazorpayPaymentId(razorpayPaymentId);
        txnRepository.save(txn);

        inAppNotificationService.createNotification(
                txn.getUserId(),
                null,
                "Wallet Top-up Successful",
                "₹" + txn.getAmount() + " has been added to your wallet. New balance: ₹" + newBalance,
                "WALLET_TOPUP_SUCCESS"
        );
    }


    @Transactional
    public void verifyTopupFailure(TopupFailureRequest request) {
        WalletTransaction txn = txnRepository.findByRazorpayOrderIdForUpdate(request.razorpayOrderId()).
                orElseThrow(() -> new PaymentException("Top-up order not found."));

        if (txn.getUserId() != SecurityUtils.currentUserId()) {
            throw new PaymentException("Top-up order not found.");
        }

        if (!PaymentStatus.PENDING.equals(txn.getStatus())) {
            return; // already completed or failed, nothing to do
        }

        RazorpayGateway.GatewayPayment payment = gateway.fetchPayment(request.razorpayPaymentId());

        if (!txn.getRazorpayOrderId().equals(payment.orderId()) || !payment.isFailed()) {
            throw new PaymentException("Payment failure could not be verified.");
        }

        failTopup(txn);
    }


    @Transactional
    public void failTopup(WalletTransaction txn) {
        if (PaymentStatus.PENDING.equals(txn.getStatus())) {
            txn.setStatus(PaymentStatus.FAILED);
            txnRepository.save(txn);

            inAppNotificationService.createNotification(
                    txn.getUserId(),
                    null,
                    "Wallet Top-up Failed",
                    "Your wallet top-up of ₹" + txn.getAmount() + " has failed. Please try again.",
                    "WALLET_TOPUP_FAILED"
            );
        }
    }

    // --------------------------------------------------- pay a session bill

    /**
     * Debits the wallet for a charging session. Fails (and changes nothing) when the balance is too low.
     */
    @Transactional
    public BigDecimal debitForSession(int userId, BigDecimal amount, int sessionId) {
        User user = userRepository.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException("User not found."));

        if (user.getWalletBalance().compareTo(amount) < 0) {
            throw new WalletException("Insufficient wallet balance. Please add money and try again.");
        }

        BigDecimal newBalance = user.getWalletBalance().subtract(amount);
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        txnRepository.save(newTxn(userId, WalletTransaction.DEBIT, amount, newBalance, PaymentStatus.SUCCESS, WalletTransaction.REF_SESSION_PAYMENT, sessionId, null, null));

        return newBalance;
    }

    // ---------------------------------------------------------------- helpers

    private User lockCurrentUser() {
        return userRepository.findByIdForUpdate(SecurityUtils.currentUserId()).orElseThrow(() -> new UserNotFoundException("User not found."));
    }

    private BigDecimal validateTopupAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Enter a valid amount.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amount can have at most 2 decimal places.");
        }
        BigDecimal value = amount.setScale(2, RoundingMode.UNNECESSARY);
        if (value.compareTo(minTopup) < 0) {
            throw new IllegalArgumentException("Minimum top-up amount is ₹" + minTopup.toPlainString() + ".");
        }
        if (value.compareTo(maxTopup) > 0) {
            throw new IllegalArgumentException("Maximum top-up amount is ₹" + maxTopup.toPlainString() + ".");
        }
        return value;
    }

    static long toPaise(BigDecimal rupees) {
        return rupees.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private WalletTransaction newTxn(int userId, String type, BigDecimal amount, BigDecimal balanceAfter, String status, String referenceType, Integer referenceId, String razorpayOrderId, String razorpayPaymentId) {
        WalletTransaction txn = new WalletTransaction();
        txn.setUserId(userId);
        txn.setTxnType(type);
        txn.setAmount(amount);
        txn.setBalanceAfter(balanceAfter);
        txn.setStatus(status);
        txn.setReferenceType(referenceType);
        txn.setReferenceId(referenceId);
        txn.setRazorpayOrderId(razorpayOrderId);
        txn.setRazorpayPaymentId(razorpayPaymentId);
        txn.setCreatedAt(LocalDateTime.now(clock));
        return txn;
    }
}
