package com.readersnetwork.bookshelf.controller;

import com.readersnetwork.bookshelf.dto.response.UserResponse;
import com.readersnetwork.bookshelf.entity.User;
import com.readersnetwork.bookshelf.service.UserService;
import com.readersnetwork.bookshelf.service.UserService.UserStats;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    @Autowired
    private UserService userService;

    // ============================================
    // USER PROFILE
    // ============================================

    /**
     * Get current authenticated user
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Long userId = getCurrentUserId();
            User user = userService.getUserById(userId);
            UserResponse response = convertToResponse(user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }
    }

    /**
     * Get user by ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            UserResponse response = convertToResponse(user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user by username
     * GET /api/users/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(convertToResponse(user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    /**
     * Update user profile
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        try {
            // Check if current user is updating their own profile
            Long currentUserId = getCurrentUserId();
            if (!currentUserId.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Cannot update another user's profile"));
            }

            User user = userService.updateProfile(
                    id,
                    request.getFullName(),
                    request.getBio(),
                    request.getProfilePictureUrl());

            UserResponse response = convertToResponse(user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update privacy settings
     * PATCH /api/users/{id}/privacy
     */
    @PatchMapping("/{id}/privacy")
    public ResponseEntity<?> updatePrivacySettings(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            Long currentUserId = getCurrentUserId();
            if (!currentUserId.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Cannot update another user's privacy settings"));
            }

            Boolean isPrivate = request.get("isPrivate");
            User user = userService.updatePrivacySettings(id, isPrivate);
            UserResponse response = convertToResponse(user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Change password
     * POST /api/users/{id}/change-password
     */
    @PostMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordChangeRequest request) {
        try {
            Long currentUserId = getCurrentUserId();
            if (!currentUserId.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Cannot change another user's password"));
            }

            userService.changePassword(
                    id,
                    request.getOldPassword(),
                    request.getNewPassword());

            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Search users
     * GET /api/users/search?query=john&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userService.searchUsers(query, pageable);
        Page<UserResponse> responses = users.map(this::convertToResponse);

        return ResponseEntity.ok(responses);
    }

    // ============================================
    // FOLLOW SYSTEM
    // ============================================

    /**
     * Follow a user
     * POST /api/users/{id}/follow
     */
    @PostMapping("/{id}/follow")
    public ResponseEntity<?> followUser(@PathVariable Long id) {
        try {
            Long currentUserId = getCurrentUserId();
            userService.followUser(currentUserId, id);

            return ResponseEntity.ok(Map.of(
                    "message", "Successfully followed user",
                    "isFollowing", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Unfollow a user
     * DELETE /api/users/{id}/follow
     */
    @DeleteMapping("/{id}/follow")
    public ResponseEntity<?> unfollowUser(@PathVariable Long id) {
        try {
            Long currentUserId = getCurrentUserId();
            userService.unfollowUser(currentUserId, id);

            return ResponseEntity.ok(Map.of(
                    "message", "Successfully unfollowed user",
                    "isFollowing", false));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if current user follows another user
     * GET /api/users/{id}/follow/status
     */
    @GetMapping("/{id}/follow/status")
    public ResponseEntity<?> getFollowStatus(@PathVariable Long id) {
        try {
            Long currentUserId = getCurrentUserId();
            boolean isFollowing = userService.isFollowing(currentUserId, id);

            return ResponseEntity.ok(Map.of("isFollowing", isFollowing));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of("isFollowing", false));
        }
    }

    /**
     * Get users that this user follows
     * GET /api/users/{id}/following
     */
    @GetMapping("/{id}/following")
    public ResponseEntity<Page<UserResponse>> getFollowing(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> following = userService.getFollowing(id, pageable);
        Page<UserResponse> responses = following.map(this::convertToResponse);

        return ResponseEntity.ok(responses);
    }

    /**
     * Get users following this user
     * GET /api/users/{id}/followers
     */
    @GetMapping("/{id}/followers")
    public ResponseEntity<Page<UserResponse>> getFollowers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> followers = userService.getFollowers(id, pageable);
        Page<UserResponse> responses = followers.map(this::convertToResponse);

        return ResponseEntity.ok(responses);
    }

    /**
     * Get user statistics
     * GET /api/users/{id}/stats
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<UserStats> getUserStats(@PathVariable Long id) {
        UserStats stats = userService.getUserStats(id);
        return ResponseEntity.ok(stats);
    }

    // ============================================
    // MUTUAL FOLLOWERS & SUGGESTIONS
    // ============================================

    /**
     * Get mutual followers
     * GET /api/users/{id}/mutual-followers
     */
    @GetMapping("/{id}/mutual-followers")
    public ResponseEntity<List<UserResponse>> getMutualFollowers(@PathVariable Long id) {
        List<User> mutualFollowers = userService.getMutualFollowers(id);
        List<UserResponse> responses = mutualFollowers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get suggested users to follow
     * GET /api/users/suggestions
     */
    @GetMapping("/suggestions")
    public ResponseEntity<Page<UserResponse>> getSuggestedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long currentUserId = getCurrentUserId();
            Pageable pageable = PageRequest.of(page, size);
            Page<User> suggestions = userService.suggestUsersToFollow(currentUserId, pageable);
            Page<UserResponse> responses = suggestions.map(this::convertToResponse);

            return ResponseEntity.ok(responses);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Get most active users
     * GET /api/users/active
     */
    @GetMapping("/active")
    public ResponseEntity<Page<UserResponse>> getMostActiveUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> activeUsers = userService.getMostActiveUsers(pageable);
        Page<UserResponse> responses = activeUsers.map(this::convertToResponse);

        return ResponseEntity.ok(responses);
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Get current authenticated user ID from security context
     */
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

    /**
     * Convert User entity to UserResponse
     */
    private UserResponse convertToResponse(User user) {
        UserStats stats = userService.getUserStats(user.getId());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getDisplayName())
                .bio(user.getBio())
                .profilePictureUrl(user.getAvatarUrl())
                .followersCount((int) stats.getFollowersCount())
                .followingCount((int) stats.getFollowingCount())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(null) // Add this field to User entity if needed
                .build();
    }

    // ============================================
    // INNER DTOs (for requests not covered by existing DTOs)
    // ============================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserUpdateRequest {
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        private String fullName;

        @Size(max = 500, message = "Bio must not exceed 500 characters")
        private String bio;

        private String profilePictureUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordChangeRequest {
        @NotBlank(message = "Old password is required")
        private String oldPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "New password must be at least 6 characters")
        private String newPassword;
    }
}