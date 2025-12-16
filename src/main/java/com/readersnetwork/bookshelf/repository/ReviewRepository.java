package com.readersnetwork.bookshelf.repository;

import com.readersnetwork.bookshelf.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);

    List<Review> findByUserId(Long userId);

    List<Review> findByBookId(Long bookId);

    Page<Review> findByBookId(Long bookId, Pageable pageable);

    Page<Review> findByUserId(Long userId, Pageable pageable);

    // Get recent reviews ordered by date
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Get reviews from users that a user follows
    @Query("SELECT r FROM Review r WHERE r.user.id IN " +
            "(SELECT uf.following.id FROM UserFollow uf WHERE uf.follower.id = :userId) " +
            "ORDER BY r.createdAt DESC")
    Page<Review> findReviewsFromFollowing(@Param("userId") Long userId, Pageable pageable);

    // Get highest rated reviews for a book
    @Query("SELECT r FROM Review r WHERE r.book.id = :bookId " +
            "ORDER BY r.likeCount DESC, r.createdAt DESC")
    Page<Review> findPopularReviewsForBook(@Param("bookId") Long bookId, Pageable pageable);

    // Average rating for a book
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId")
    Double findAverageRatingForBook(@Param("bookId") Long bookId);

    // Count reviews for a book
    long countByBookId(Long bookId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);
}