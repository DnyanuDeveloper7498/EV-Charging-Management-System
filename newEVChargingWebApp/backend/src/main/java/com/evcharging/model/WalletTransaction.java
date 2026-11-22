package com.evcharging.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ledger entry for every wallet movement (top-ups, session payments, refunds). */
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    public static final String CREDIT = "CREDIT";
    public static final String DEBIT = "DEBIT";
    public static final String REFUND = "REFUND";

    public static final String REF_TOPUP = "TOPUP";
    public static final String REF_TOPUP_DEMO = "TOPUP_DEMO";
    public static final String REF_SESSION_PAYMENT = "SESSION_PAYMENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_id")
    private int txnId;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "txn_type", nullable = false)
    private String txnType;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after")
    private BigDecimal balanceAfter;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reference_type", nullable = false)
    private String referenceType;

    @Column(name = "reference_id")
    private Integer referenceId;

    @Column(name = "razorpay_order_id",unique = true)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public WalletTransaction() {
    }

    public int getTxnId() {
        return txnId;
    }

    public void setTxnId(int txnId) {
        this.txnId = txnId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
