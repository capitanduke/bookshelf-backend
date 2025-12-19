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
public class BookshelfBookResponse {

    private Long id;
    private Long bookshelfId;
    private String bookshelfName;
    private BookResponse book;
    private Integer position;
    private LocalDateTime addedAt;
}