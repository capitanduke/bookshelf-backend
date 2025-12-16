package com.readersnetwork.bookshelf.repository;

import com.readersnetwork.bookshelf.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    Optional<ReviewLike> findByUserIdAndReviewId(Long userId, Long reviewId);

    boolean existsByUserIdAndReviewId(Long userId, Long reviewId);

    void deleteByUserIdAndReviewId(Long userId, Long reviewId);

    long countByReviewId(Long reviewId);

    List<ReviewLike> findByReviewId(Long reviewId);

    List<ReviewLike> findByUserId(Long userId);
}