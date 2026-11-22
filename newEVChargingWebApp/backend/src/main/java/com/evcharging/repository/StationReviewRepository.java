package com.evcharging.repository;

import com.evcharging.model.StationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StationReviewRepository extends JpaRepository<StationReview, Integer> {

    List<StationReview> findByStationIdOrderByCreatedAtDesc(int stationId);

    Optional<StationReview> findByStationIdAndUserId(int stationId, int userId);

    boolean existsByUserId(int userId);

    /** Rows of [stationId, average rating, review count]. */
    @Query("select r.stationId, avg(r.rating), count(r) from StationReview r group by r.stationId")
    List<Object[]> summarize();
}
