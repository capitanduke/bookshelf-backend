package com.readersnetwork.bookshelf.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFollowRequest {
    @NotNull(message = "Following user ID is required")
    private Long followingId;
}
