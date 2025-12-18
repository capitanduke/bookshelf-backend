package com.readersnetwork.bookshelf.repository;

import com.readersnetwork.bookshelf.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByUsernameContainingIgnoreCase(String username);

    // Find users by favorite genre
    @Query("SELECT u FROM User u WHERE u.favoriteGenres LIKE %:genre%")
    List<User> findByFavoriteGenre(@Param("genre") String genre);

    // Search users by username or display name
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    // Find users that the given user follows
    @Query("SELECT f.following FROM UserFollow f WHERE f.follower.id = :userId")
    Page<User> findFollowing(@Param("userId") Long userId, Pageable pageable);

    // Find users following the given user
    @Query("SELECT f.follower FROM UserFollow f WHERE f.following.id = :userId")
    Page<User> findFollowers(@Param("userId") Long userId, Pageable pageable);

    // Count books for a user
    @Query("SELECT COUNT(ub) FROM UserBook ub WHERE ub.user.id = :userId")
    long countBooksByUserId(@Param("userId") Long userId);

    // Count reviews for a user
    @Query("SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId")
    long countReviewsByUserId(@Param("userId") Long userId);

    // Find mutual followers
    @Query("SELECT u FROM User u WHERE " +
            "u.id IN (SELECT f1.follower.id FROM UserFollow f1 WHERE f1.following.id = :userId) AND " +
            "u.id IN (SELECT f2.following.id FROM UserFollow f2 WHERE f2.follower.id = :userId)")
    List<User> findMutualFollowers(@Param("userId") Long userId);

    // Suggest users (users who have read the same books)
    @Query("SELECT DISTINCT ub2.user FROM UserBook ub2 WHERE " +
            "ub2.book.id IN (SELECT ub1.book.id FROM UserBook ub1 WHERE ub1.user.id = :userId) AND " +
            "ub2.user.id <> :userId")
    Page<User> findSuggestedUsers(@Param("userId") Long userId, Pageable pageable);

    // Find most active users (most reviews)
    @Query("SELECT r.user FROM Review r GROUP BY r.user ORDER BY COUNT(r) DESC")
    Page<User> findMostActiveUsers(Pageable pageable);
}