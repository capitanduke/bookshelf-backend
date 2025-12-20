package com.readersnetwork.bookshelf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFollowStats {
    private Long userId;
    private long followersCount;
    private long followingCount;
    private boolean isFollowing; // Is the current user following this user?
    private boolean isFollowedBy; // Is this user following the current user?
    private boolean isMutual; // Are they following each other?
}
