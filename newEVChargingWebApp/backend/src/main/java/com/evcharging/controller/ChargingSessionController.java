package com.evcharging.controller;

import com.evcharging.dto.StartSessionRequest;
import com.evcharging.model.ChargingSession;
import com.evcharging.security.SecurityUtils;
import com.evcharging.service.ChargingSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class ChargingSessionController {

    private final ChargingSessionService chargingSessionService;

    public ChargingSessionController(ChargingSessionService chargingSessionService) {
        this.chargingSessionService = chargingSessionService;
    }

    /**
     * The caller's own sessions. "?userId=" is accepted for compatibility but must be the caller's own id
     * (admins may ask for any user). Everyone else's data is never returned.
     */
    @GetMapping
    public List<ChargingSession> getSessions(@RequestParam(required = false) Integer userId) {
        int target = userId == null ? SecurityUtils.currentUserId() : userId;
        return chargingSessionService.getSessionsForUser(target);
    }

    //---------- ADMIN only-----------
    @GetMapping("/all")
    public List<ChargingSession> getAllSessions() {
        return chargingSessionService.getAllSessions();
    }

    //---------- ADMIN only-----------

    @GetMapping("/active")
    public List<ChargingSession> getActiveSessions() {
        return chargingSessionService.getActiveSessions();
    }

    @GetMapping("/{sessionId}")
    public ChargingSession getSessionById(@PathVariable int sessionId) {
        return chargingSessionService.getSessionById(sessionId);
    }

    @GetMapping("/booking/{bookingId}")
    public ChargingSession getSessionByBookingId(@PathVariable int bookingId) {
        return chargingSessionService.getSessionByBookingId(bookingId);
    }

    @PostMapping
    public ResponseEntity<ChargingSession> startSession(@Valid @RequestBody StartSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chargingSessionService.startSession(request));
    }

    @PostMapping("/stop")
    public ChargingSession stopSession(@RequestParam int sessionId, @RequestParam double endBatteryPercentage) {
        return chargingSessionService.stopSession(sessionId, endBatteryPercentage);
    }
}
