package com.readersnetwork.bookshelf.service;

import com.readersnetwork.bookshelf.dto.request.UserFollowRequest;
import com.readersnetwork.bookshelf.dto.response.UserFollowResponse;
import com.readersnetwork.bookshelf.dto.response.UserFollowStats;
import com.readersnetwork.bookshelf.dto.response.UserResponse;
import com.readersnetwork.bookshelf.entity.User;
import com.readersnetwork.bookshelf.entity.UserFollow;
import com.readersnetwork.bookshelf.exception.ResourceNotFoundException;
import com.readersnetwork.bookshelf.exception.ValidationException;
import com.readersnetwork.bookshelf.repository.UserFollowRepository;
import com.readersnetwork.bookshelf.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    /**
     * Follow a user
     */
    @Transactional
    public UserFollowResponse followUser(Long followerId, @NonNull UserFollowRequest requestDTO) {
        log.info("User {} attempting to follow user {}", followerId, requestDTO.getFollowingId());

        // Validate that users can't follow themselves
        if (followerId.equals(requestDTO.getFollowingId())) {
            throw new ValidationException("You cannot follow yourself");
        }

        // Check if already following
        if (userFollowRepository.existsByFollowerIdAndFollowingId(followerId, requestDTO.getFollowingId())) {
            throw new ValidationException("You are already following this user");
        }

        // Get both users
        User follower = userRepository.findById(Objects.requireNonNull(followerId))
                .orElseThrow(() -> new ResourceNotFoundException("Follower user not found"));

        User following = userRepository.findById(Objects.requireNonNull(requestDTO.getFollowingId()))
                .orElseThrow(() -> new ResourceNotFoundException("User to follow not found"));

        // Create follow relationship
        UserFollow userFollow = UserFollow.builder()
                .follower(follower)
                .following(following)
                .build();

        UserFollow savedFollow = userFollowRepository.save(Objects.requireNonNull(userFollow));
        log.info("User {} successfully followed user {}", followerId, requestDTO.getFollowingId());

        return mapToResponseDTO(savedFollow);
    }

    /**
     * Unfollow a user
     */
    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        log.info("User {} attempting to unfollow user {}", followerId, followingId);

        if (!userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new ResourceNotFoundException("Follow relationship not found");
        }

        userFollowRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        log.info("User {} successfully unfollowed user {}", followerId, followingId);
    }

    /**
     * Get users that a specific user follows
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getFollowing(Long userId, Pageable pageable) {
        log.debug("Getting users followed by user {}", userId);

        if (!userRepository.existsById(Objects.requireNonNull(userId))) {
            throw new ResourceNotFoundException("User not found");
        }

        Page<User> following = userFollowRepository.findFollowing(userId, pageable);
        return following.map(this::mapToUserResponse);
    }

    /**
     * Get users that follow a specific user
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getFollowers(Long userId, Pageable pageable) {
        log.debug("Getting followers of user {}", userId);

        if (!userRepository.existsById(Objects.requireNonNull(userId))) {
            throw new ResourceNotFoundException("User not found");
        }

        Page<User> followers = userFollowRepository.findFollowers(userId, pageable);
        return followers.map(this::mapToUserResponse);
    }

    /**
     * Get mutual follows (friends) - users who follow each other
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getMutualFollows(Long userId) {
        log.debug("Getting mutual follows for user {}", userId);

        if (!userRepository.existsById(Objects.requireNonNull(userId))) {
            throw new ResourceNotFoundException("User not found");
        }

        List<User> mutualFollows = userFollowRepository.findMutualFollows(userId);
        return mutualFollows.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get follow statistics for a user
     */
    @Transactional(readOnly = true)
    public UserFollowStats getFollowStats(Long userId, Long currentUserId) {
        log.debug("Getting follow stats for user {}", userId);

        if (!userRepository.existsById(Objects.requireNonNull(userId))) {
            throw new ResourceNotFoundException("User not found");
        }

        long followersCount = userFollowRepository.countByFollowingId(userId);
        long followingCount = userFollowRepository.countByFollowerId(userId);

        boolean isFollowing = false;
        boolean isFollowedBy = false;

        if (currentUserId != null && !currentUserId.equals(userId)) {
            isFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, userId);
            isFollowedBy = userFollowRepository.existsByFollowerIdAndFollowingId(userId, currentUserId);
        }

        return UserFollowStats.builder()
                .userId(userId)
                .followersCount(followersCount)
                .followingCount(followingCount)
                .isFollowing(isFollowing)
                .isFollowedBy(isFollowedBy)
                .isMutual(isFollowing && isFollowedBy)
                .build();
    }

    /**
     * Check if a user is following another user
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        return userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    // Mapping methods
    private UserFollowResponse mapToResponseDTO(UserFollow userFollow) {
        return UserFollowResponse.builder()
                .id(userFollow.getId())
                .follower(mapToUserResponse(userFollow.getFollower()))
                .following(mapToUserResponse(userFollow.getFollowing()))
                .createdAt(userFollow.getCreatedAt())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getUsername())
                .bio(user.getBio())
                .profilePictureUrl(user.getAvatarUrl())
                .followersCount(null) // Can be populated if needed
                .followingCount(null) // Can be populated if needed
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getUpdatedAt())
                .build();
    }
}