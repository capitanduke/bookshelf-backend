package com.readersnetwork.bookshelf.service;

import com.readersnetwork.bookshelf.dto.response.ReviewLikeResponse;
import com.readersnetwork.bookshelf.entity.Review;
import com.readersnetwork.bookshelf.entity.ReviewLike;
import com.readersnetwork.bookshelf.entity.User;
import com.readersnetwork.bookshelf.exception.DuplicateResourceException;
import com.readersnetwork.bookshelf.exception.ReviewNotFoundException;
import com.readersnetwork.bookshelf.exception.ResourceNotFoundException;
import com.readersnetwork.bookshelf.exception.UserNotFoundException;
import com.readersnetwork.bookshelf.repository.ReviewLikeRepository;
import com.readersnetwork.bookshelf.repository.ReviewRepository;
import com.readersnetwork.bookshelf.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewLikeService {

    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    // ============= LIKE OPERATIONS =============

    /**
     * Like a review. Creates a like if it doesn't exist.
     * Also increments the review's like count.
     */
    @Transactional
    public ReviewLikeResponse likeReview(@NonNull Long userId, @NonNull Long reviewId) {
        // Check if already liked
        if (reviewLikeRepository.existsByUserIdAndReviewId(userId, reviewId)) {
            throw new DuplicateResourceException("You have already liked this review");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        // Create the like
        ReviewLike reviewLike = ReviewLike.builder()
                .user(user)
                .review(review)
                .build();

        @SuppressWarnings("null")
        ReviewLike savedLike = Objects.requireNonNull(
                reviewLikeRepository.save(reviewLike),
                "ReviewLike save operation returned null");

        // Increment like count on the review
        review.setLikeCount(review.getLikeCount() + 1);
        reviewRepository.save(review);

        return convertToResponse(savedLike);
    }

    /**
     * Unlike a review. Removes the like if it exists.
     * Also decrements the review's like count.
     */
    @Transactional
    public void unlikeReview(@NonNull Long userId, @NonNull Long reviewId) {
        ReviewLike reviewLike = reviewLikeRepository.findByUserIdAndReviewId(userId, reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Like not found for this review"));

        // Delete the like
        reviewLikeRepository.delete(reviewLike);

        // Decrement like count on the review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setLikeCount(Math.max(0, review.getLikeCount() - 1)); // Ensure it doesn't go negative
        reviewRepository.save(review);
    }

    /**
     * Toggle like status. If liked, unlike. If not liked, like.
     * Returns true if the review is now liked, false if unliked.
     */
    @Transactional
    public boolean toggleLike(@NonNull Long userId, @NonNull Long reviewId) {
        if (reviewLikeRepository.existsByUserIdAndReviewId(userId, reviewId)) {
            unlikeReview(userId, reviewId);
            return false;
        } else {
            likeReview(userId, reviewId);
            return true;
        }
    }

    /**
     * Check if a user has liked a specific review
     */
    public boolean hasUserLikedReview(@NonNull Long userId, @NonNull Long reviewId) {
        return reviewLikeRepository.existsByUserIdAndReviewId(userId, reviewId);
    }

    /**
     * Get all users who liked a specific review
     */
    public List<ReviewLikeResponse> getUsersWhoLikedReview(@NonNull Long reviewId) {
        // Verify review exists
        if (!reviewRepository.existsById(reviewId)) {
            throw new ReviewNotFoundException("Review not found with id: " + reviewId);
        }

        List<ReviewLike> likes = reviewLikeRepository.findByReviewId(reviewId);
        return likes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all reviews liked by a specific user
     */
    public List<ReviewLikeResponse> getReviewsLikedByUser(@NonNull Long userId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        List<ReviewLike> likes = reviewLikeRepository.findByUserId(userId);
        return likes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get the count of likes for a specific review
     */
    public long getLikeCountForReview(@NonNull Long reviewId) {
        return reviewLikeRepository.countByReviewId(reviewId);
    }

    /**
     * Get a specific like by user and review
     */
    public ReviewLikeResponse getLike(@NonNull Long userId, @NonNull Long reviewId) {
        ReviewLike reviewLike = reviewLikeRepository.findByUserIdAndReviewId(userId, reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Like not found"));

        return convertToResponse(reviewLike);
    }

    // ============= HELPER METHODS =============

    private ReviewLikeResponse convertToResponse(ReviewLike reviewLike) {
        return ReviewLikeResponse.builder()
                .id(reviewLike.getId())
                .userId(reviewLike.getUser().getId())
                .username(reviewLike.getUser().getUsername())
                .userProfilePicture(reviewLike.getUser().getAvatarUrl())
                .reviewId(reviewLike.getReview().getId())
                .createdAt(reviewLike.getCreatedAt())
                .build();
    }
}