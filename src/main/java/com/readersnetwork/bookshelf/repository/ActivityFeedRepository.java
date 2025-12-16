package com.readersnetwork.bookshelf.repository;

import com.readersnetwork.bookshelf.entity.ActivityFeed;
import com.readersnetwork.bookshelf.entity.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityFeedRepository extends JpaRepository<ActivityFeed, Long> {

    // Get user's own activities
    Page<ActivityFeed> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Get activities by type
    Page<ActivityFeed> findByActivityTypeOrderByCreatedAtDesc(ActivityType type, Pageable pageable);

    // Get feed for a user (activities from users they follow)
    @Query("SELECT a FROM ActivityFeed a WHERE a.user.id IN " +
            "(SELECT uf.following.id FROM UserFollow uf WHERE uf.follower.id = :userId) " +
            "ORDER BY a.createdAt DESC")
    Page<ActivityFeed> findFeedForUser(@Param("userId") Long userId, Pageable pageable);

    // Get combined feed (user's own + following)
    @Query("SELECT a FROM ActivityFeed a WHERE a.user.id = :userId " +
            "OR a.user.id IN (SELECT uf.following.id FROM UserFollow uf WHERE uf.follower.id = :userId) " +
            "ORDER BY a.createdAt DESC")
    Page<ActivityFeed> findCombinedFeed(@Param("userId") Long userId, Pageable pageable);

    // Get activities in date range
    @Query("SELECT a FROM ActivityFeed a WHERE a.createdAt BETWEEN :start AND :end " +
            "ORDER BY a.createdAt DESC")
    List<ActivityFeed> findActivitiesInDateRange(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Count activities by user
    long countByUserId(Long userId);

    // Get recent activities (last N days)
    @Query("SELECT a FROM ActivityFeed a WHERE a.user.id = :userId " +
            "AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<ActivityFeed> findRecentActivities(@Param("userId") Long userId,
            @Param("since") LocalDateTime since);
}