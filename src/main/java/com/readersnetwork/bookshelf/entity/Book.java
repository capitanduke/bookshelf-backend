package com.readersnetwork.bookshelf.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "books", indexes = {
        @Index(name = "idx_isbn", columnList = "isbn"),
        @Index(name = "idx_title_author", columnList = "title,author"),
        @Index(name = "idx_google_books_id", columnList = "google_books_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String author;

    // ISBN-13 or ISBN-10 (unique identifier for books)
    @Column(unique = true, length = 13)
    private String isbn;

    // External API identifiers (for avoiding duplicates)
    @Column(name = "google_books_id", unique = true, length = 50)
    private String googleBooksId; // e.g., "zyTCAlFPjgYC"

    @Column(name = "open_library_id", length = 50)
    private String openLibraryId; // e.g., "OL7353617M"

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(length = 2000)
    private String description;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(length = 100)
    private String genre;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(length = 100)
    private String publisher;

    @Column(length = 50)
    private String language; // e.g., "en", "es", "fr"

    // Track where the book came from
    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 30)
    private BookSource source; // GOOGLE_BOOKS, OPEN_LIBRARY, MANUAL_ENTRY

    @Builder.Default
    @Column(name = "is_verified")
    private Boolean isVerified = false; // Manually verified by admin

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @Builder.Default
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserBook> userBooks = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BookshelfBook> bookshelfBooks = new HashSet<>();
}