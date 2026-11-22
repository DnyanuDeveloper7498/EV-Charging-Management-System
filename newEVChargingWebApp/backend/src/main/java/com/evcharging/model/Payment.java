package com.evcharging.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private int paymentId;

    @Column(name = "session_id", nullable = false, unique = true)
    private int sessionId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Column(name = "razorpay_order_id", unique = true)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", unique = true)
    private String razorpayPaymentId;

    @Column(name = "failure_reason")
    private String failureReason;

    public Payment() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    // Getter names are intentionally unchanged (JSON contract with the frontend).
    public int getpaymentId() {
        return paymentId;
    }

    public void setpaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public String getpaymentMethod() {
        return paymentMethod;
    }

    public void setpaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getpaymentStatus() {
        return paymentStatus;
    }

    public void setpaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getpaymentTime() {
        return paymentTime;
    }

    public void setpaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public int getsessionId() {
        return sessionId;
    }

    public void setsessionId(int sessionId) {
        this.sessionId = sessionId;
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

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    @Override
    public String toString() {
        return "Payment{paymentId=" + paymentId + ", sessionId=" + sessionId + ", amount=" + amount
                + ", paymentMethod='" + paymentMethod + "', paymentStatus='" + paymentStatus + "'}";
    }
}
