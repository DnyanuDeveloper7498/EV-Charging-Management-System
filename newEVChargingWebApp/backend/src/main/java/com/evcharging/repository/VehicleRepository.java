package com.evcharging.repository;

import com.evcharging.model.Vehicle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    List<Vehicle> findByUserId(int userId);

    long countByUserId(int userId);

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    boolean existsByVehicleNumber(String vehicleNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.vehicleId = :vehicleId")
    Optional<Vehicle> findByIdForUpdate(@Param("vehicleId") int vehicleId);
}
