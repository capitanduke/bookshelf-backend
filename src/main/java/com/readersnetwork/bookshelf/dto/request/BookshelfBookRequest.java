package com.readersnetwork.bookshelf.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookshelfBookRequest {

    @NotNull(message = "Book ID is required")
    private Long bookId;

    @Min(value = 0, message = "Position must be non-negative")
    private Integer position;
}