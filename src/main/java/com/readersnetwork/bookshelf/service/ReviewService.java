package com.readersnetwork.bookshelf.service;

import com.readersnetwork.bookshelf.dto.response.ReviewResponse;
import com.readersnetwork.bookshelf.dto.request.ReviewRequest;
import com.readersnetwork.bookshelf.dto.request.ReviewUpdateRequest;
import com.readersnetwork.bookshelf.entity.Review;

import com.readersnetwork.bookshelf.entity.User;
import com.readersnetwork.bookshelf.entity.Book;
import com.readersnetwork.bookshelf.exception.BookNotFoundException;
import com.readersnetwork.bookshelf.exception.UserNotFoundException;
import com.readersnetwork.bookshelf.exception.ReviewNotFoundException;
import com.readersnetwork.bookshelf.exception.DuplicateResourceException;
import com.readersnetwork.bookshelf.exception.InvalidOperationException;
import com.readersnetwork.bookshelf.exception.UnauthorizedAccessException;
import org.springframework.lang.NonNull;
import com.readersnetwork.bookshelf.repository.ReviewRepository;

import com.readersnetwork.bookshelf.repository.UserRepository;
import com.readersnetwork.bookshelf.repository.BookRepository;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;

    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    // ============= REVIEW OPERATIONS =============

    @Transactional
    @SuppressWarnings("null")
    public ReviewResponse createReview(@NonNull Long userId, @NonNull ReviewRequest request) {
        Long bookId = Objects.requireNonNull(request.getBookId(), "Book ID cannot be null");

        // Check if user already reviewed this book
        if (reviewRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw new DuplicateResourceException("You have already reviewed this book");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + bookId));

        Review review = Review.builder()
                .user(user)
                .book(book)
                .rating(request.getRating())
                .title(request.getTitle())
                .content(request.getContent())
                .containsSpoilers(request.getContainsSpoilers() != null ? request.getContainsSpoilers() : false)
                .likeCount(0)
                .build();

        return convertToResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse updateReview(@NonNull Long reviewId, @NonNull Long userId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only update your own reviews");
        }

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getTitle() != null) {
            review.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            review.setContent(request.getContent());
        }
        if (request.getContainsSpoilers() != null) {
            review.setContainsSpoilers(request.getContainsSpoilers());
        }

        Review updated = reviewRepository.save(review);
        return convertToResponse(updated);
    }

    @Transactional
    public void deleteReview(@NonNull Long reviewId, @NonNull Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    public ReviewResponse getReviewById(@NonNull Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        return convertToResponse(review);
    }

    public Page<ReviewResponse> getReviewsForBook(@NonNull Long bookId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByBookId(bookId, pageable);
        return reviews.map(this::convertToResponse);
    }

    public Page<ReviewResponse> getReviewsByUser(@NonNull Long userId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByUserId(userId, pageable);
        return reviews.map(this::convertToResponse);
    }

    public Page<ReviewResponse> getPopularReviewsForBook(@NonNull Long bookId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findPopularReviewsForBook(bookId, pageable);
        return reviews.map(this::convertToResponse);
    }

    public Page<ReviewResponse> getRecentReviews(Pageable pageable) {
        Page<Review> reviews = reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        return reviews.map(this::convertToResponse);
    }

    public Page<ReviewResponse> getReviewsFromFollowing(@NonNull Long userId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findReviewsFromFollowing(userId, pageable);
        return reviews.map(this::convertToResponse);
    }

    public Double getAverageRatingForBook(@NonNull Long bookId) {
        return reviewRepository.findAverageRatingForBook(bookId);
    }

    public long getReviewCountForBook(@NonNull Long bookId) {
        return reviewRepository.countByBookId(bookId);
    }

    private ReviewResponse convertToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .username(review.getUser().getUsername())
                .userProfilePicture(review.getUser().getAvatarUrl())
                .bookId(review.getBook().getId())
                .bookTitle(review.getBook().getTitle())
                .bookAuthor(review.getBook().getAuthor())
                .bookCoverImage(review.getBook().getCoverUrl())
                .rating(review.getRating())
                .title(review.getTitle()) // Note: Ensure Review entity has this field
                .content(review.getContent())
                .containsSpoilers(review.getContainsSpoilers()) // Note: Ensure Review entity has this field
                .likesCount(review.getLikeCount())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}