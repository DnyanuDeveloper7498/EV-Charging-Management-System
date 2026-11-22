package com.evcharging.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private int vehicleId;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Column(name = "brand")
    private String brand;

    @Column(name = "model")
    private String model;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "battery_percentage")
    private double batteryPercentage;

    @Column(name = "user_id")
    private int userId;

    @Column(name = "battery_capacity")
    private double batteryCapacity;

    public Vehicle() {
    }

    public Vehicle(double batteryPercentage, String brand, String model,
                   int userId, String vehicleType, String vehicleNumber,
                   double batteryCapacity) {
        this.batteryPercentage = batteryPercentage;
        this.brand = brand;
        this.model = model;
        this.userId = userId;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.batteryCapacity = batteryCapacity;
    }

    public Vehicle(double batteryPercentage, String brand, String model,
                   int userId, int vehicleId, String vehicleType,
                   String vehicleNumber, double batteryCapacity) {
        this.batteryPercentage = batteryPercentage;
        this.brand = brand;
        this.model = model;
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.batteryCapacity = batteryCapacity;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public double getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(double batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(double batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    @Override
    public String toString() {
        return brand + " " + model + " - " + vehicleNumber;
    }
}