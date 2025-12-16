package com.readersnetwork.bookshelf.repository;

import com.readersnetwork.bookshelf.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // ============================================
    // DUPLICATE PREVENTION (Critical for API integration)
    // ============================================

    Optional<Book> findByIsbn(String isbn);

    Optional<Book> findByGoogleBooksId(String googleBooksId);

    Optional<Book> findByOpenLibraryId(String openLibraryId);

    // Find by title AND author (best way to avoid duplicates without ISBN)
    @Query("SELECT b FROM Book b WHERE LOWER(TRIM(b.title)) = LOWER(TRIM(:title)) " +
            "AND LOWER(TRIM(b.author)) = LOWER(TRIM(:author))")
    Optional<Book> findByTitleAndAuthor(@Param("title") String title,
            @Param("author") String author);

    // Check if book exists by any identifier (use this before saving!)
    @Query("SELECT b FROM Book b WHERE b.isbn = :isbn " +
            "OR b.googleBooksId = :googleBooksId " +
            "OR b.openLibraryId = :openLibraryId " +
            "OR (LOWER(TRIM(b.title)) = LOWER(TRIM(:title)) " +
            "AND LOWER(TRIM(b.author)) = LOWER(TRIM(:author)))")
    Optional<Book> findByAnyIdentifier(@Param("isbn") String isbn,
            @Param("googleBooksId") String googleBooksId,
            @Param("openLibraryId") String openLibraryId,
            @Param("title") String title,
            @Param("author") String author);

    // ============================================
    // SEARCH & DISCOVERY
    // ============================================

    List<Book> findByTitleContainingIgnoreCase(String title);

    List<Book> findByAuthorContainingIgnoreCase(String author);

    List<Book> findByGenre(String genre);

    Page<Book> findByGenre(String genre, Pageable pageable);

    // Search books by title or author (fuzzy search)
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Book> searchBooks(@Param("query") String query, Pageable pageable);

    // Advanced search with multiple filters
    @Query("SELECT b FROM Book b WHERE " +
            "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
            "(:genre IS NULL OR b.genre = :genre) AND " +
            "(:year IS NULL OR b.publishedYear = :year)")
    Page<Book> advancedSearch(@Param("title") String title,
            @Param("author") String author,
            @Param("genre") String genre,
            @Param("year") Integer year,
            Pageable pageable);

    // ============================================
    // STATISTICS & RECOMMENDATIONS
    // ============================================

    // Get most reviewed books (popular books)
    @Query("SELECT b FROM Book b LEFT JOIN b.reviews r " +
            "GROUP BY b ORDER BY COUNT(r) DESC")
    Page<Book> findMostReviewedBooks(Pageable pageable);

    // Get highest rated books (quality books)
    @Query("SELECT b FROM Book b LEFT JOIN b.reviews r " +
            "GROUP BY b HAVING COUNT(r) >= :minReviews " +
            "ORDER BY AVG(r.rating) DESC")
    Page<Book> findHighestRatedBooks(@Param("minReviews") long minReviews, Pageable pageable);

    // Get recently added books (new arrivals)
    Page<Book> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Get trending books (recently reviewed)
    @Query("SELECT r.book FROM Review r " +
            "WHERE r.createdAt >= :since " +
            "GROUP BY r.book ORDER BY COUNT(r) DESC")
    List<Book> findTrendingBooks(@Param("since") java.time.LocalDateTime since,
            Pageable pageable);

    // ============================================
    // VALIDATION & ADMIN
    // ============================================

    boolean existsByIsbn(String isbn);

    boolean existsByGoogleBooksId(String googleBooksId);

    // Count books by source (for admin dashboard)
    long countBySource(com.readersnetwork.bookshelf.entity.BookSource source);
}