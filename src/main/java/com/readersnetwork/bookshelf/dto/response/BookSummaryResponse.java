package com.readersnetwork.bookshelf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookSummaryResponse {

    private Long id;
    private String title;
    private String author;
    private String coverImageUrl;
    private Double averageRating;
    private Integer reviewCount;
}