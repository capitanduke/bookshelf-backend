package com.readersnetwork.bookshelf.dto.request;

import com.readersnetwork.bookshelf.entity.PrivacyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookshelfRequest {

    @NotBlank(message = "Bookshelf name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private PrivacyLevel privacy;
}