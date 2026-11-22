package com.evcharging.service;

import com.evcharging.dto.RatingSummary;
import com.evcharging.dto.ReviewRequest;
import com.evcharging.dto.ReviewResponse;
import com.evcharging.exception.ChargingStationNotFoundException;
import com.evcharging.model.StationReview;
import com.evcharging.model.User;
import com.evcharging.repository.ChargingStationRepository;
import com.evcharging.repository.StationReviewRepository;
import com.evcharging.repository.UserRepository;
import com.evcharging.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final StationReviewRepository reviewRepository;
    private final ChargingStationRepository stationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ReviewService(StationReviewRepository reviewRepository,
                         ChargingStationRepository stationRepository,
                         UserRepository userRepository,
                         Clock clock) {
        this.reviewRepository = reviewRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listForStation(int stationId) {
        requireStation(stationId);
        List<StationReview> reviews = reviewRepository.findByStationIdOrderByCreatedAtDesc(stationId);

        Map<Integer, String> names = new HashMap<>();
        userRepository.findAllById(reviews.stream().map(StationReview::getUserId).distinct().toList())
                .forEach(u -> names.put(u.getUserId(), firstName(u)));

        return reviews.stream()
                .map(r -> new ReviewResponse(r.getReviewId(), r.getStationId(),
                        names.getOrDefault(r.getUserId(), "User"), r.getRating(), r.getComment(), r.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RatingSummary> summaries() {
        return reviewRepository.summarize().stream()
                .map(row -> new RatingSummary(
                        ((Number) row[0]).intValue(),
                        Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0,
                        ((Number) row[2]).longValue()))
                .toList();
    }

    /** One review per user per station: posting again replaces the earlier review. */
    @Transactional
    public ReviewResponse saveMine(int stationId, ReviewRequest request) {
        requireStation(stationId);
        int userId = SecurityUtils.currentUserId();

        StationReview review = reviewRepository.findByStationIdAndUserId(stationId, userId)
                .orElseGet(StationReview::new);
        review.setStationId(stationId);
        review.setUserId(userId);
        review.setRating(request.rating());
        String comment = request.comment() == null ? "" : request.comment().trim();
        review.setComment(comment.isEmpty() ? null : comment);
        review.setCreatedAt(LocalDateTime.now(clock));
        StationReview saved = reviewRepository.save(review);

        String name = userRepository.findById(userId).map(ReviewService::firstName).orElse("User");
        return new ReviewResponse(saved.getReviewId(), stationId, name, saved.getRating(), saved.getComment(),
                saved.getCreatedAt());
    }

    private void requireStation(int stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new ChargingStationNotFoundException("Charging station not found.");
        }
    }

    private static String firstName(User user) {
        String name = user.getFullName() == null ? "" : user.getFullName().trim();
        return name.isEmpty() ? "User" : name.split("\\s+")[0];
    }
}
