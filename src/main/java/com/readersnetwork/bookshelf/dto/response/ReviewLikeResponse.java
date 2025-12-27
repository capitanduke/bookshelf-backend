package com.readersnetwork.bookshelf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewLikeResponse {

    private Long id;

    private Long userId;

    private String username;

    private String userProfilePicture;

    private Long reviewId;

    private LocalDateTime createdAt;
}