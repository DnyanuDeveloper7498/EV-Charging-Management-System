package com.evcharging.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "charging_stations")
public class ChargingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "station_id")
    private int stationId;

    @Column(name = "station_name")
    private String stationName;

    @Column(name = "s_location")
    private String sLocation;

    @Column(name = "total_slots")
    private int totalSlots;

    @Column(name = "available_slots")
    private int availableSlots;

    @Column(name = "price_per_kwh")
    private BigDecimal pricePerKwt;

    @Column(name = "charger_type")
    private String chargerType;

    @Column(name = "status")
    private String status;


    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "owner_id")
    private Integer ownerId;


    @Column(name = "booking_mode", nullable = false, length = 20)
    private String bookingMode = "AUTO";


    public ChargingStation() {
    }

    // Getter names are intentionally unchanged (JSON contract with the frontend: sLocation, pricePerKwt ...).
    public int getavailableSlots() {
        return availableSlots;
    }

    public void setavailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }

    public String getchargerType() {
        return chargerType;
    }

    public void setchargerType(String chargerType) {
        this.chargerType = chargerType;
    }

    public BigDecimal getpricePerKwt() {
        return pricePerKwt;
    }

    public void setpricePerKwt(BigDecimal pricePerKwt) {
        this.pricePerKwt = pricePerKwt;
    }

    public String getsLocation() {
        return sLocation;
    }

    public void setsLocation(String sLocation) {
        this.sLocation = sLocation;
    }

    public int getstationId() {
        return stationId;
    }

    public void setstationId(int stationId) {
        this.stationId = stationId;
    }

    public String getstationName() {
        return stationName;
    }

    public void setstationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int gettotalSlots() {
        return totalSlots;
    }

    public void settotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }


    public boolean isBookable() {
        return "AVAILABLE".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status);
    }

    public String getBookingMode() {
        return bookingMode;
    }

    public void setBookingMode(String bookingMode) {
        this.bookingMode = bookingMode;
    }

    @Override
    public String toString() {
        return "ChargingStation{" + "availableSlots=" + availableSlots + ", stationId=" + stationId + ", stationName='" + stationName + '\'' + ", sLocation='" + sLocation + '\'' + ", totalSlots=" + totalSlots + ", pricePerKwt=" + pricePerKwt + ", chargerType='" + chargerType + '\'' + ", status='" + status + '\'' + ", latitude=" + latitude + ", longitude=" + longitude + '}';
    }
}
