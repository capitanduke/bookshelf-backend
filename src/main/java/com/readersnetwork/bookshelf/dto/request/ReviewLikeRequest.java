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
public class ReviewLikeRequest {

    @NotNull(message = "Review ID is required")
    private Long reviewId;
}
