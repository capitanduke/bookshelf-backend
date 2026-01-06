package com.readersnetwork.bookshelf.service;

import com.readersnetwork.bookshelf.entity.User;
import com.readersnetwork.bookshelf.entity.UserFollow;
import com.readersnetwork.bookshelf.repository.UserRepository;
import com.readersnetwork.bookshelf.repository.UserFollowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFollowRepository userFollowRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ============================================
    // SPRING SECURITY - UserDetailsService
    // ============================================

    /**
     * Load user by username for Spring Security authentication
     * Required by UserDetailsService interface
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Convert User entity to Spring Security UserDetails
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(new ArrayList<>()) // Add roles/authorities here if you have them
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    // ============================================
    // USER MANAGEMENT
    // ============================================

    /**
     * Register a new user
     * Checks: username unique, email unique
     */
    @Transactional
    @SuppressWarnings("null")
    public User registerUser(String username, String email, String password, String displayName) {
        // Validation
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered: " + email);
        }

        // Create user
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .displayName(displayName != null ? displayName : username)
                .build();

        return userRepository.save(user);
    }

    /**
     * Get user by ID
     */
    @SuppressWarnings("null")
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    /**
     * Get user by username (for login)
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get user by email (for password reset)
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Update user profile
     */
    @Transactional
    @SuppressWarnings("null")
    public User updateProfile(Long userId, String displayName, String bio, String avatarUrl) {
        User user = getUserById(userId);

        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        if (bio != null) {
            user.setBio(bio);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }

        return userRepository.save(user);
    }

    /**
     * Update privacy settings
     */
    @Transactional
    public User updatePrivacySettings(Long userId, Boolean isPrivate) {
        User user = getUserById(userId);
        user.setIsPrivate(isPrivate);
        return userRepository.save(user);
    }

    /**
     * Change password
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Incorrect current password");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Search users by username or display name
     */
    public Page<User> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable);
    }

    // ============================================
    // FOLLOW SYSTEM
    // ============================================

    /**
     * Follow a user
     * Creates UserFollow relationship
     */
    @Transactional
    @SuppressWarnings("null")
    public UserFollow followUser(Long followerId, Long followingId) {
        // Validation
        if (followerId.equals(followingId)) {
            throw new RuntimeException("Cannot follow yourself");
        }

        // Check if already following
        if (userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new RuntimeException("Already following this user");
        }

        // Check if users exist
        User follower = getUserById(followerId);
        User following = getUserById(followingId);

        // Create follow relationship
        UserFollow userFollow = UserFollow.builder()
                .follower(follower)
                .following(following)
                .build();

        return userFollowRepository.save(userFollow);
    }

    /**
     * Unfollow a user
     */
    @Transactional
    @SuppressWarnings("null")
    public void unfollowUser(Long followerId, Long followingId) {
        UserFollow follow = userFollowRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new RuntimeException("Not following this user"));

        userFollowRepository.delete(follow);
    }

    /**
     * Check if user A follows user B
     */
    public boolean isFollowing(Long followerId, Long followingId) {
        return userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    /**
     * Get users that this user follows
     */
    public Page<User> getFollowing(Long userId, Pageable pageable) {
        return userRepository.findFollowing(userId, pageable);
    }

    /**
     * Get users following this user
     */
    public Page<User> getFollowers(Long userId, Pageable pageable) {
        return userRepository.findFollowers(userId, pageable);
    }

    /**
     * Get follower/following counts
     */
    public UserStats getUserStats(Long userId) {
        long followersCount = userFollowRepository.countByFollowingId(userId);
        long followingCount = userFollowRepository.countByFollowerId(userId);
        long booksCount = userRepository.countBooksByUserId(userId);
        long reviewsCount = userRepository.countReviewsByUserId(userId);

        return new UserStats(followersCount, followingCount, booksCount, reviewsCount);
    }

    // ============================================
    // MUTUAL FOLLOWERS & SUGGESTIONS
    // ============================================

    /**
     * Get mutual followers (users who follow each other)
     */
    public List<User> getMutualFollowers(Long userId) {
        return userRepository.findMutualFollowers(userId);
    }

    /**
     * Suggest users to follow (users with similar reading tastes)
     * Logic: Users who liked the same books
     */
    public Page<User> suggestUsersToFollow(Long userId, Pageable pageable) {
        return userRepository.findSuggestedUsers(userId, pageable);
    }

    /**
     * Get most active users (for discovery)
     */
    public Page<User> getMostActiveUsers(Pageable pageable) {
        return userRepository.findMostActiveUsers(pageable);
    }

    // ============================================
    // INNER CLASS: User Statistics
    // ============================================

    public static class UserStats {
        private long followersCount;
        private long followingCount;
        private long booksCount;
        private long reviewsCount;

        public UserStats(long followersCount, long followingCount, long booksCount, long reviewsCount) {
            this.followersCount = followersCount;
            this.followingCount = followingCount;
            this.booksCount = booksCount;
            this.reviewsCount = reviewsCount;
        }

        // Getters
        public long getFollowersCount() {
            return followersCount;
        }

        public long getFollowingCount() {
            return followingCount;
        }

        public long getBooksCount() {
            return booksCount;
        }

        public long getReviewsCount() {
            return reviewsCount;
        }
    }
}