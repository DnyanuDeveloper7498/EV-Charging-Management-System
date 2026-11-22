package com.evcharging.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "station_reviews")
public class StationReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private int reviewId;

    @Column(name = "station_id")
    private int stationId;

    @Column(name = "user_id")
    private int userId;

    @Column(name = "rating")
    private int rating;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public StationReview() {
    }

    public int getReviewId() { return reviewId; }

    public int getStationId() { return stationId; }

    public void setStationId(int stationId) { this.stationId = stationId; }

    public int getUserId() { return userId; }

    public void setUserId(int userId) { this.userId = userId; }

    public int getRating() { return rating; }

    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }

    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
