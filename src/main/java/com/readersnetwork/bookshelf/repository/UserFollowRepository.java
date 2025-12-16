package com.readersnetwork.bookshelf.repository;

import com.readersnetwork.bookshelf.entity.User;
import com.readersnetwork.bookshelf.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // Get all users that a user follows
    @Query("SELECT uf.following FROM UserFollow uf WHERE uf.follower.id = :userId")
    Page<User> findFollowing(@Param("userId") Long userId, Pageable pageable);

    // Get all users that follow a user
    @Query("SELECT uf.follower FROM UserFollow uf WHERE uf.following.id = :userId")
    Page<User> findFollowers(@Param("userId") Long userId, Pageable pageable);

    // Count followers
    long countByFollowingId(Long userId);

    // Count following
    long countByFollowerId(Long userId);

    // Get mutual follows (friends)
    @Query("SELECT uf1.following FROM UserFollow uf1 " +
            "WHERE uf1.follower.id = :userId " +
            "AND EXISTS (SELECT uf2 FROM UserFollow uf2 " +
            "WHERE uf2.follower.id = uf1.following.id AND uf2.following.id = :userId)")
    List<User> findMutualFollows(@Param("userId") Long userId);
}