package com.evcharging.dto;

import java.time.LocalDateTime;

/** Public view of a review: shows the reviewer's first name only, never the e-mail. */
public record ReviewResponse(
        int reviewId,
        int stationId,
        String reviewer,
        int rating,
        String comment,
        LocalDateTime createdAt) {
}
