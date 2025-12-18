package com.readersnetwork.bookshelf.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewUpdateRequest {

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    private Integer rating;

    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    private Boolean containsSpoilers;
}