package com.readersnetwork.bookshelf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author must not exceed 255 characters")
    private String author;

    @Size(max = 20, message = "ISBN must not exceed 20 characters")
    private String isbn;

    private LocalDate publicationDate;

    @Size(max = 100, message = "Publisher must not exceed 100 characters")
    private String publisher;

    private Integer pageCount;

    @Size(max = 50, message = "Language must not exceed 50 characters")
    private String language;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private String coverImageUrl;

    private Set<String> genres;
}