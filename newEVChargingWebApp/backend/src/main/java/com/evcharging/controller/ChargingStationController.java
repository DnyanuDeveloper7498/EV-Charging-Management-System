package com.evcharging.controller;

import com.evcharging.dto.RatingSummary;
import com.evcharging.dto.ReviewRequest;
import com.evcharging.dto.ReviewResponse;
import com.evcharging.dto.StationRequest;
import com.evcharging.model.ChargingStation;
import com.evcharging.service.ChargingStationService;
import com.evcharging.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stations")
public class ChargingStationController {

    private final ChargingStationService chargingStationService;
    private final ReviewService reviewService;

    public ChargingStationController(ChargingStationService chargingStationService, ReviewService reviewService) {
        this.chargingStationService = chargingStationService;
        this.reviewService = reviewService;
    }

    /** ADMIN / STATION_OWNER only (enforced in SecurityConfig). */
    @PostMapping
    public ResponseEntity<ChargingStation> addStation(@Valid @RequestBody StationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chargingStationService.addStation(request));
    }

    @GetMapping
    public List<ChargingStation> getAllStations() {
        return chargingStationService.getAllStations();
    }

    /** Average rating + number of reviews for every reviewed station (public). */
    @GetMapping("/ratings")
    public List<RatingSummary> ratings() {
        return reviewService.summaries();
    }

    @GetMapping("/{stationId}")
    public ChargingStation getStationById(@PathVariable int stationId) {
        return chargingStationService.findByStationId(stationId);
    }

    /** ADMIN, or the STATION_OWNER who created the station (checked in the service). */
    @PutMapping("/{stationId}")
    public ChargingStation updateStation(@PathVariable int stationId, @Valid @RequestBody StationRequest request) {
        return chargingStationService.updateStation(stationId, request);
    }

    /** ADMIN, or the STATION_OWNER who created the station. Stations with booking history cannot be deleted. */
    @DeleteMapping("/{stationId}")
    public ResponseEntity<Map<String, String>> deleteStation(@PathVariable int stationId) {
        chargingStationService.deleteStation(stationId);
        return ResponseEntity.ok(Map.of("message", "Station deleted."));
    }

    /** ADMIN only (enforced in SecurityConfig). */
    @PutMapping("/{stationId}/slots")
    public ResponseEntity<Void> updateAvailableSlots(@PathVariable int stationId, @RequestParam int availableSlots) {
        chargingStationService.updateAvailableSlots(stationId, availableSlots);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{stationId}/reviews")
    public List<ReviewResponse> reviews(@PathVariable int stationId) {
        return reviewService.listForStation(stationId);
    }

    /** Any logged-in user; posting again updates the caller's own review. */
    @PostMapping("/{stationId}/reviews")
    public ReviewResponse saveReview(@PathVariable int stationId, @Valid @RequestBody ReviewRequest request) {
        return reviewService.saveMine(stationId, request);
    }
}
