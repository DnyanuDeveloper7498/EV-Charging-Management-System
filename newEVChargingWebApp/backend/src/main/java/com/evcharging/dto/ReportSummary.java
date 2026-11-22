package com.evcharging.dto;

import java.math.BigDecimal;

/**
 * scope = SYSTEM (admin): totalUsers/totalStations/totalRevenue are filled.
 * scope = PERSONAL (everyone else): only the caller's own data; system-only fields are null and totalSpent is filled.
 */
public record ReportSummary(
        String scope,
        Long totalUsers,
        long totalVehicles,
        Long totalStations,
        long totalBookings,
        long totalSessions,
        BigDecimal totalChargingUnits,
        BigDecimal totalRevenue,
        BigDecimal totalSpent) {
}
