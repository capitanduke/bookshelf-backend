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
public class UserProfileResponse {

    private Long id;
    private String username;
    private String fullName;
    private String bio;
    private String profilePictureUrl;
    private Integer followersCount;
    private Integer followingCount;
    private Integer bookshelvesCount;
    private Integer reviewsCount;
    private LocalDateTime createdAt;
}
