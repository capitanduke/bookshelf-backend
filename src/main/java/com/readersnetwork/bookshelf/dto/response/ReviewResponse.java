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
public class ReviewResponse {

    private Long id;
    private Long userId;
    private String username;
    private String userProfilePicture;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookCoverImage;
    private Integer rating;
    private String title;
    private String content;
    private Boolean containsSpoilers;
    private Integer likesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}