package com.evcharging.repository;

import com.evcharging.model.ChargingStation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChargingStationRepository extends JpaRepository<ChargingStation, Integer> {

    /** Row lock - serialises slot allocation so the same slot can never be given to two users. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ChargingStation s where s.stationId = :stationId")
    Optional<ChargingStation> findByIdForUpdate(@Param("stationId") int stationId);

    boolean existsByOwnerId(Integer ownerId);
}

