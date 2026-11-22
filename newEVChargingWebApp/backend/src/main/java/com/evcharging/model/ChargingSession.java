package com.evcharging.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "charging_sessions")
public class ChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private int sessionId;

    @Column(name = "booking_id")
    private int bookingId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "units_consumed")
    private BigDecimal unitsConsumed = BigDecimal.ZERO;

    @Column(name = "total_cost")
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "start_battery_percentage")
    private double startBatteryPercentage;

    @Column(name = "end_battery_percentage")
    private double endBatteryPercentage;

    public ChargingSession() {
    }

    public double getStartBatteryPercentage() {
        return startBatteryPercentage;
    }

    public void setStartBatteryPercentage(double startBatteryPercentage) {
        this.startBatteryPercentage = startBatteryPercentage;
    }

    public double getEndBatteryPercentage() {
        return endBatteryPercentage;
    }

    public void setEndBatteryPercentage(double endBatteryPercentage) {
        this.endBatteryPercentage = endBatteryPercentage;
    }

    // Getter names are intentionally unchanged (JSON contract with the frontend).
    public int getbookingId() {
        return bookingId;
    }

    public void setbookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public LocalDateTime getendTime() {
        return endTime;
    }

    public void setendTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getsessionId() {
        return sessionId;
    }

    public void setsessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getstartTime() {
        return startTime;
    }

    public void setstartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public BigDecimal gettotalCost() {
        return totalCost;
    }

    public void settotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getunitsConsumed() {
        return unitsConsumed;
    }

    public void setunitsConsumed(BigDecimal unitsConsumed) {
        this.unitsConsumed = unitsConsumed;
    }

    @Override
    public String toString() {
        return "ChargingSession{sessionId=" + sessionId + ", bookingId=" + bookingId + ", startTime=" + startTime
                + ", endTime=" + endTime + ", unitsConsumed=" + unitsConsumed + ", totalCost=" + totalCost + "}";
    }
}
