package com.readersnetwork.bookshelf.dto.response;

import com.readersnetwork.bookshelf.entity.BookSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String googleBooksId;
    private String openLibraryId;
    private String coverUrl;
    private String description;
    private Integer publishedYear;
    private String genre;
    private Integer pageCount;
    private Double averageRating;
    private String publisher;
    private String language;
    private BookSource source;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}