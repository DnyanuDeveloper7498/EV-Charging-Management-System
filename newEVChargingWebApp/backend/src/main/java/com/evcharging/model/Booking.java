package com.evcharging.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private int bookingId;

    @Column(name = "vehicle_id")
    private int vehicleId;

    @Column(name = "station_id")
    private int stationId;

    @Column(name = "booking_time")
    private LocalDateTime bookingTime;

    @Column(name = "booking_status")
    private String bookingStatus;


    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_type")
    private String cancellationType;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "refund_status")
    private String refundStatus = "NOT_APPLICABLE";

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Column(name = "approval_expires_at")
    private LocalDateTime approvalExpiresAt;

    public Booking() {
    }

    public Booking(String bookingStatus, LocalDateTime bookingTime, int stationId, int vehicleId) {
        this.bookingStatus = bookingStatus;
        this.bookingTime = bookingTime;
        this.stationId = stationId;
        this.vehicleId = vehicleId;
    }

    // NOTE: getter names are kept as-is because Jackson derives the JSON property names
    // (bookingId, vehicleId, ...) from them and the frontend depends on those names.
    public int getbookingId() {
        return bookingId;
    }

    public void setbookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getbookingStatus() {
        return bookingStatus;
    }

    public void setbookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public LocalDateTime getbookingTime() {
        return bookingTime;
    }

    public void setbookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public int getstationId() {
        return stationId;
    }

    public void setstationId(int stationId) {
        this.stationId = stationId;
    }

    public int getvehicleId() {
        return vehicleId;
    }

    public void setvehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }


    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationType() {
        return cancellationType;
    }

    public void setCancellationType(String cancellationType) {
        this.cancellationType = cancellationType;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(LocalDateTime reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }

    public LocalDateTime getApprovalExpiresAt() {
        return approvalExpiresAt;
    }

    public void setApprovalExpiresAt(LocalDateTime approvalExpiresAt) {
        this.approvalExpiresAt = approvalExpiresAt;
    }

    @Override
    public String toString() {
        return "Booking{bookingId=" + bookingId + ", vehicleId=" + vehicleId + ", stationId=" + stationId + ", bookingTime=" + bookingTime + ", bookingStatus='" + bookingStatus + "'}";
    }
}
