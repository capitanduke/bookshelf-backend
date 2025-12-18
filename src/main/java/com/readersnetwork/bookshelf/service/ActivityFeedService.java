package com.readersnetwork.bookshelf.service;

import com.readersnetwork.bookshelf.entity.*;
import com.readersnetwork.bookshelf.repository.ActivityFeedRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityFeedService {

    @Autowired
    private ActivityFeedRepository activityFeedRepository;

    @Autowired
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Target type constants
    private static final String TARGET_TYPE_BOOK = "BOOK";
    private static final String TARGET_TYPE_REVIEW = "REVIEW";
    private static final String TARGET_TYPE_USER = "USER";
    private static final String TARGET_TYPE_BOOKSHELF = "BOOKSHELF";

    // ============================================
    // CREATE ACTIVITIES
    // ============================================

    /**
     * Create activity for book-related actions
     * Examples: ADDED_TO_LIBRARY, STARTED_READING, FINISHED_BOOK
     */
    @Transactional
    @SuppressWarnings("null")
    public ActivityFeed createBookActivity(Long userId, ActivityType activityType, Long bookId,
            Map<String, Object> metadata) {
        User user = userService.getUserById(userId);

        ActivityFeed activity = ActivityFeed.builder()
                .user(user)
                .activityType(activityType)
                .targetId(bookId)
                .targetType(TARGET_TYPE_BOOK)
                .metadata(serializeMetadata(metadata))
                .build();

        return activityFeedRepository.save(activity);
    }

    /**
     * Create activity for review
     */
    @Transactional
    @SuppressWarnings("null")
    public ActivityFeed createReviewActivity(Long userId, Long reviewId, Long bookId, Integer rating) {
        User user = userService.getUserById(userId);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bookId", bookId);
        metadata.put("rating", rating);

        ActivityFeed activity = ActivityFeed.builder()
                .user(user)
                .activityType(ActivityType.POSTED_REVIEW)
                .targetId(reviewId)
                .targetType(TARGET_TYPE_REVIEW)
                .metadata(serializeMetadata(metadata))
                .build();

        return activityFeedRepository.save(activity);
    }

    /**
     * Create activity for bookshelf
     */
    @Transactional
    @SuppressWarnings("null")
    public ActivityFeed createBookshelfActivity(Long userId, ActivityType activityType,
            Long bookshelfId, Long bookId) {
        User user = userService.getUserById(userId);

        Map<String, Object> metadata = new HashMap<>();
        if (bookId != null) {
            metadata.put("bookId", bookId);
        }

        ActivityFeed activity = ActivityFeed.builder()
                .user(user)
                .activityType(activityType)
                .targetId(bookshelfId)
                .targetType(TARGET_TYPE_BOOKSHELF)
                .metadata(serializeMetadata(metadata))
                .build();

        return activityFeedRepository.save(activity);
    }

    /**
     * Create activity for following a user
     */
    @Transactional
    @SuppressWarnings("null")
    public ActivityFeed createFollowActivity(Long followerId, Long followingId) {
        User follower = userService.getUserById(followerId);

        ActivityFeed activity = ActivityFeed.builder()
                .user(follower)
                .activityType(ActivityType.FOLLOWED_USER)
                .targetId(followingId)
                .targetType(TARGET_TYPE_USER)
                .build();

        return activityFeedRepository.save(activity);
    }

    /**
     * Generic activity creation with metadata
     */
    @Transactional
    @SuppressWarnings("null")
    public ActivityFeed createActivity(Long userId, ActivityType activityType,
            Long targetId, String targetType,
            Map<String, Object> metadata) {
        User user = userService.getUserById(userId);

        ActivityFeed activity = ActivityFeed.builder()
                .user(user)
                .activityType(activityType)
                .targetId(targetId)
                .targetType(targetType)
                .metadata(serializeMetadata(metadata))
                .build();

        return activityFeedRepository.save(activity);
    }

    // ============================================
    // QUERY ACTIVITY FEED
    // ============================================

    /**
     * Get user's own activity feed (profile page)
     */
    public Page<ActivityFeed> getUserActivities(Long userId, Pageable pageable) {
        return activityFeedRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get feed from followed users only (following feed)
     */
    public Page<ActivityFeed> getFollowingFeed(Long userId, Pageable pageable) {
        return activityFeedRepository.findFeedForUser(userId, pageable);
    }

    /**
     * Get combined feed (user's own + following)
     * This is the main "Home Feed"
     */
    public Page<ActivityFeed> getCombinedFeed(Long userId, Pageable pageable) {
        return activityFeedRepository.findCombinedFeed(userId, pageable);
    }

    /**
     * Get activities by type (e.g., all reviews)
     */
    public Page<ActivityFeed> getActivitiesByType(ActivityType activityType, Pageable pageable) {
        return activityFeedRepository.findByActivityTypeOrderByCreatedAtDesc(activityType, pageable);
    }

    /**
     * Get recent activities for user (last N days)
     */
    public List<ActivityFeed> getRecentActivities(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return activityFeedRepository.findRecentActivities(userId, since);
    }

    /**
     * Get activities in date range (for analytics)
     */
    public List<ActivityFeed> getActivitiesInDateRange(LocalDateTime start, LocalDateTime end) {
        return activityFeedRepository.findActivitiesInDateRange(start, end);
    }

    // ============================================
    // STATISTICS & INSIGHTS
    // ============================================

    /**
     * Get activity statistics for a user
     */
    public ActivityStats getActivityStats(Long userId) {
        long totalActivities = activityFeedRepository.countByUserId(userId);

        // Count activities by type (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<ActivityFeed> recentActivities = activityFeedRepository.findRecentActivities(
                userId, thirtyDaysAgo);

        long booksAdded = recentActivities.stream()
                .filter(a -> a.getActivityType() == ActivityType.ADDED_TO_BOOKSHELF)
                .count();

        long booksStarted = recentActivities.stream()
                .filter(a -> a.getActivityType() == ActivityType.STARTED_BOOK)
                .count();

        long booksFinished = recentActivities.stream()
                .filter(a -> a.getActivityType() == ActivityType.FINISHED_BOOK)
                .count();

        long reviewsWritten = recentActivities.stream()
                .filter(a -> a.getActivityType() == ActivityType.POSTED_REVIEW)
                .count();

        long bookshelvesCreated = recentActivities.stream()
                .filter(a -> a.getActivityType() == ActivityType.CREATED_BOOKSHELF)
                .count();

        return new ActivityStats(
                totalActivities,
                recentActivities.size(),
                booksAdded,
                booksStarted,
                booksFinished,
                reviewsWritten,
                bookshelvesCreated);
    }

    /**
     * Get most active users (by activity count in last 30 days)
     */
    public List<Map<String, Object>> getMostActiveUsers(int limit) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime now = LocalDateTime.now();

        List<ActivityFeed> recentActivities = activityFeedRepository
                .findActivitiesInDateRange(thirtyDaysAgo, now);

        // Group by user and count activities
        Map<Long, Long> userActivityCount = new HashMap<>();
        for (ActivityFeed activity : recentActivities) {
            Long userId = activity.getUser().getId();
            userActivityCount.put(userId, userActivityCount.getOrDefault(userId, 0L) + 1);
        }

        // Sort and return top users
        return userActivityCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("userId", entry.getKey());
                    result.put("activityCount", entry.getValue());
                    return result;
                })
                .toList();
    }

    /**
     * Get trending books from activity feed (last 7 days)
     */
    public List<Map<String, Object>> getTrendingBooks(int limit) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime now = LocalDateTime.now();

        List<ActivityFeed> recentActivities = activityFeedRepository
                .findActivitiesInDateRange(sevenDaysAgo, now);

        // Count book-related activities
        Map<Long, Long> bookActivityCount = new HashMap<>();
        for (ActivityFeed activity : recentActivities) {
            if (TARGET_TYPE_BOOK.equals(activity.getTargetType())) {
                Long bookId = activity.getTargetId();
                bookActivityCount.put(bookId, bookActivityCount.getOrDefault(bookId, 0L) + 1);
            } else if (TARGET_TYPE_REVIEW.equals(activity.getTargetType())) {
                // Extract bookId from metadata
                Map<String, Object> metadata = deserializeMetadata(activity.getMetadata());
                if (metadata != null && metadata.containsKey("bookId")) {
                    Long bookId = ((Number) metadata.get("bookId")).longValue();
                    bookActivityCount.put(bookId, bookActivityCount.getOrDefault(bookId, 0L) + 1);
                }
            }
        }

        // Sort and return top books
        return bookActivityCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("bookId", entry.getKey());
                    result.put("activityCount", entry.getValue());
                    return result;
                })
                .toList();
    }

    // ============================================
    // ACTIVITY DETAILS & ENRICHMENT
    // ============================================

    /**
     * Get enriched activity with full details
     * Resolves targetId to actual entity data
     */
    public ActivityDetails getActivityDetails(long activityId) {
        ActivityFeed activity = activityFeedRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        ActivityDetails details = new ActivityDetails();
        details.setActivity(activity);
        details.setMetadata(deserializeMetadata(activity.getMetadata()));

        return details;
    }

    /**
     * Format activity as human-readable text
     */
    public String formatActivity(ActivityFeed activity) {
        String userName = activity.getUser().getDisplayName();

        return switch (activity.getActivityType()) {
            case STARTED_BOOK -> userName + " started reading a book";
            case FINISHED_BOOK -> userName + " finished reading a book";
            case POSTED_REVIEW -> userName + " wrote a review";
            case CREATED_BOOKSHELF -> userName + " created a new bookshelf";
            case ADDED_TO_BOOKSHELF -> userName + " added a book to a bookshelf";
            case FOLLOWED_USER -> userName + " followed a user";
            default -> userName + " performed an action";
        };
    }

    // ============================================
    // HELPER METHODS - JSON METADATA
    // ============================================

    /**
     * Serialize metadata map to JSON string
     */
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserialize JSON string to metadata map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(metadataJson, Map.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // ============================================
    // CLEANUP & MAINTENANCE
    // ============================================

    /**
     * Delete old activities (for data retention)
     */
    @SuppressWarnings("null")
    @Transactional
    public int deleteOldActivities(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<ActivityFeed> oldActivities = activityFeedRepository
                .findActivitiesInDateRange(LocalDateTime.of(2000, 1, 1, 0, 0), cutoffDate);

        activityFeedRepository.deleteAll(oldActivities);
        return oldActivities.size();
    }

    // ============================================
    // INNER CLASSES
    // ============================================

    public static class ActivityStats {
        private long totalActivities;
        private long recentActivities; // Last 30 days
        private long booksAdded;
        private long booksStarted;
        private long booksFinished;
        private long reviewsWritten;
        private long bookshelvesCreated;

        public ActivityStats(long totalActivities, long recentActivities, long booksAdded,
                long booksStarted, long booksFinished, long reviewsWritten,
                long bookshelvesCreated) {
            this.totalActivities = totalActivities;
            this.recentActivities = recentActivities;
            this.booksAdded = booksAdded;
            this.booksStarted = booksStarted;
            this.booksFinished = booksFinished;
            this.reviewsWritten = reviewsWritten;
            this.bookshelvesCreated = bookshelvesCreated;
        }

        // Getters
        public long getTotalActivities() {
            return totalActivities;
        }

        public long getRecentActivities() {
            return recentActivities;
        }

        public long getBooksAdded() {
            return booksAdded;
        }

        public long getBooksStarted() {
            return booksStarted;
        }

        public long getBooksFinished() {
            return booksFinished;
        }

        public long getReviewsWritten() {
            return reviewsWritten;
        }

        public long getBookshelvesCreated() {
            return bookshelvesCreated;
        }

        public double getCompletionRate() {
            if (booksStarted == 0)
                return 0.0;
            return (booksFinished * 100.0) / booksStarted;
        }
    }

    public static class ActivityDetails {
        private ActivityFeed activity;
        private Map<String, Object> metadata;

        public ActivityFeed getActivity() {
            return activity;
        }

        public void setActivity(ActivityFeed activity) {
            this.activity = activity;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}