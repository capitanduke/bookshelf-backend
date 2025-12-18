package com.readersnetwork.bookshelf.dto.response;

import com.readersnetwork.bookshelf.entity.PrivacyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookshelfResponse {

    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String description;
    private PrivacyLevel privacy;
    private Integer bookCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}