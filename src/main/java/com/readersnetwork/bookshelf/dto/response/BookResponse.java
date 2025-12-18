package com.readersnetwork.bookshelf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private LocalDate publicationDate;
    private String publisher;
    private Integer pageCount;
    private String language;
    private String description;
    private String coverImageUrl;
    private Set<String> genres;
    private Double averageRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}