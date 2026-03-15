package com.readersnetwork.bookshelf.controller;

import com.readersnetwork.bookshelf.dto.request.ReviewRequest;
import com.readersnetwork.bookshelf.dto.request.ReviewUpdateRequest;
import com.readersnetwork.bookshelf.dto.response.ReviewLikeResponse;
import com.readersnetwork.bookshelf.dto.response.ReviewResponse;
import com.readersnetwork.bookshelf.entity.User;
import com.readersnetwork.bookshelf.service.ReviewLikeService;
import com.readersnetwork.bookshelf.service.ReviewService;
import com.readersnetwork.bookshelf.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewLikeService reviewLikeService;
    private final UserService userService;

    // ============================================
    // REVIEW CRUD
    // ============================================

    /**
     * Create a new review
     * POST /api/reviews
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        Long userId = getCurrentUserId();
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a review by ID
     * GET /api/reviews/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        ReviewResponse response = reviewService.getReviewById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a review (only by the author)
     * PUT /api/reviews/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewUpdateRequest request) {
        Long userId = getCurrentUserId();
        ReviewResponse response = reviewService.updateReview(id, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a review (only by the author)
     * DELETE /api/reviews/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        reviewService.deleteReview(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // REVIEW QUERIES
    // ============================================

    /**
     * Get all reviews for a specific book
     * GET /api/reviews/book/{bookId}?page=0&size=10
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsForBook(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponse> reviews = reviewService.getReviewsForBook(bookId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get all reviews by a specific user
     * GET /api/reviews/user/{userId}?page=0&size=10
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponse> reviews = reviewService.getReviewsByUser(userId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get popular reviews for a book (sorted by likes)
     * GET /api/reviews/book/{bookId}/popular?page=0&size=10
     */
    @GetMapping("/book/{bookId}/popular")
    public ResponseEntity<Page<ReviewResponse>> getPopularReviewsForBook(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponse> reviews = reviewService.getPopularReviewsForBook(bookId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get recent reviews across all books
     * GET /api/reviews/recent?page=0&size=10
     */
    @GetMapping("/recent")
    public ResponseEntity<Page<ReviewResponse>> getRecentReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponse> reviews = reviewService.getRecentReviews(pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get reviews from users I follow (feed)
     * GET /api/reviews/following?page=0&size=10
     */
    @GetMapping("/following")
    public ResponseEntity<Page<ReviewResponse>> getReviewsFromFollowing(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponse> reviews = reviewService.getReviewsFromFollowing(userId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get average rating for a book
     * GET /api/reviews/book/{bookId}/rating
     */
    @GetMapping("/book/{bookId}/rating")
    public ResponseEntity<Map<String, Object>> getBookRating(@PathVariable Long bookId) {
        Double averageRating = reviewService.getAverageRatingForBook(bookId);
        long reviewCount = reviewService.getReviewCountForBook(bookId);
        return ResponseEntity.ok(Map.of(
                "averageRating", averageRating != null ? averageRating : 0.0,
                "reviewCount", reviewCount));
    }

    // ============================================
    // REVIEW LIKES
    // ============================================

    /**
     * Like a review
     * POST /api/reviews/{reviewId}/likes
     */
    @PostMapping("/{reviewId}/likes")
    public ResponseEntity<ReviewLikeResponse> likeReview(@PathVariable Long reviewId) {
        Long userId = getCurrentUserId();
        ReviewLikeResponse response = reviewLikeService.likeReview(userId, reviewId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Unlike a review
     * DELETE /api/reviews/{reviewId}/likes
     */
    @DeleteMapping("/{reviewId}/likes")
    public ResponseEntity<Void> unlikeReview(@PathVariable Long reviewId) {
        Long userId = getCurrentUserId();
        reviewLikeService.unlikeReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle like on a review
     * POST /api/reviews/{reviewId}/likes/toggle
     */
    @PostMapping("/{reviewId}/likes/toggle")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long reviewId) {
        Long userId = getCurrentUserId();
        boolean isLiked = reviewLikeService.toggleLike(userId, reviewId);
        return ResponseEntity.ok(Map.of(
                "liked", isLiked,
                "message", isLiked ? "Review liked" : "Review unliked"));
    }

    /**
     * Check if current user has liked a review
     * GET /api/reviews/{reviewId}/likes/status
     */
    @GetMapping("/{reviewId}/likes/status")
    public ResponseEntity<Map<String, Boolean>> getLikeStatus(@PathVariable Long reviewId) {
        Long userId = getCurrentUserId();
        boolean hasLiked = reviewLikeService.hasUserLikedReview(userId, reviewId);
        return ResponseEntity.ok(Map.of("liked", hasLiked));
    }

    /**
     * Get all users who liked a review
     * GET /api/reviews/{reviewId}/likes
     */
    @GetMapping("/{reviewId}/likes")
    public ResponseEntity<List<ReviewLikeResponse>> getUsersWhoLikedReview(
            @PathVariable Long reviewId) {
        List<ReviewLikeResponse> likes = reviewLikeService.getUsersWhoLikedReview(reviewId);
        return ResponseEntity.ok(likes);
    }

    /**
     * Get like count for a review
     * GET /api/reviews/{reviewId}/likes/count
     */
    @GetMapping("/{reviewId}/likes/count")
    public ResponseEntity<Map<String, Long>> getLikeCount(@PathVariable Long reviewId) {
        long count = reviewLikeService.getLikeCountForReview(reviewId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Not authenticated");
        }
        String username = authentication.getName();
        return userService.getUserByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
