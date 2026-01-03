package com.readersnetwork.bookshelf.controller;

import com.readersnetwork.bookshelf.dto.request.BookRequest;
import com.readersnetwork.bookshelf.dto.response.BookResponse;
import com.readersnetwork.bookshelf.entity.Book;
import com.readersnetwork.bookshelf.entity.BookSource;
import com.readersnetwork.bookshelf.exception.BookNotFoundException;
import com.readersnetwork.bookshelf.service.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "*")
public class BookController {

    @Autowired
    private BookService bookService;

    /**
     * Search for a book (checks database first, then calls API if not found)
     * GET /api/books/search?query=Harry Potter
     * 
     * This is the MAIN search endpoint users will use
     */
    @GetMapping("/search")
    public ResponseEntity<BookResponse> searchAndCreateBook(@RequestParam String query) {
        try {
            Book book = bookService.searchAndCreateBook(query);
            return ResponseEntity.ok(mapToResponse(book));
        } catch (RuntimeException e) {
            throw new BookNotFoundException("Book not found: " + query);
        }
    }

    /**
     * Get multiple search results (database only - fast search)
     * GET /api/books/search-results?query=fantasy&page=0&size=10
     * 
     * Use this for search results pages
     */
    @GetMapping("/search-results")
    public ResponseEntity<Page<BookResponse>> searchBooks(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BookResponse> results = bookService.searchBooks(query, pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(results);
    }

    /**
     * Create a book manually (when not found in APIs)
     * POST /api/books
     * 
     * Body: BookRequest JSON
     * 
     * This allows users to add books that aren't in Google Books API
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        // Create Book entity from request
        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .publisher(request.getPublisher())
                .pageCount(request.getPageCount())
                .language(request.getLanguage())
                .description(request.getDescription())
                .coverUrl(request.getCoverImageUrl())
                .genre(request.getGenres() != null && !request.getGenres().isEmpty()
                        ? String.join(", ", request.getGenres())
                        : null)
                .publishedYear(request.getPublicationDate() != null
                        ? request.getPublicationDate().getYear()
                        : null)
                .source(BookSource.MANUAL_ENTRY)
                .isVerified(false) // Needs admin verification
                .averageRating(0.0)
                .createdAt(LocalDateTime.now())
                .build();

        // Check for duplicates before saving
        Book savedBook = bookService.createBookManually(book);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedBook));
    }

    /**
     * Get book by ID
     * GET /api/books/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        Book book = bookService.getBookById(id);
        return ResponseEntity.ok(mapToResponse(book));
    }

    /**
     * Update book details (admin only)
     * PUT /api/books/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest request) {

        Book existingBook = bookService.getBookById(id);

        // Update fields
        existingBook.setTitle(request.getTitle());
        existingBook.setAuthor(request.getAuthor());
        existingBook.setIsbn(request.getIsbn());
        existingBook.setPublisher(request.getPublisher());
        existingBook.setPageCount(request.getPageCount());
        existingBook.setLanguage(request.getLanguage());
        existingBook.setDescription(request.getDescription());
        existingBook.setCoverUrl(request.getCoverImageUrl());
        existingBook.setGenre(request.getGenres() != null && !request.getGenres().isEmpty()
                ? String.join(", ", request.getGenres())
                : null);
        existingBook.setPublishedYear(request.getPublicationDate() != null
                ? request.getPublicationDate().getYear()
                : null);

        Book updatedBook = bookService.updateBook(existingBook);
        return ResponseEntity.ok(mapToResponse(updatedBook));
    }

    /**
     * Delete book (admin only)
     * DELETE /api/books/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Book deleted successfully");
        response.put("bookId", id.toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Advanced search with multiple filters
     * GET
     * /api/books/advanced-search?title=harry&author=rowling&genre=Fantasy&year=2001
     */
    @GetMapping("/advanced-search")
    public ResponseEntity<Page<BookResponse>> advancedSearch(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BookResponse> results = bookService.advancedSearch(title, author, genre, year, pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(results);
    }

    /**
     * Get books by genre
     * GET /api/books/genre/Fantasy?page=0&size=10
     */
    @GetMapping("/genre/{genre}")
    public ResponseEntity<Page<BookResponse>> getBooksByGenre(
            @PathVariable String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BookResponse> books = bookService.getBooksByGenre(genre, pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(books);
    }

    /**
     * Get most reviewed books (popular)
     * GET /api/books/most-reviewed?page=0&size=10
     */
    @GetMapping("/most-reviewed")
    public ResponseEntity<Page<BookResponse>> getMostReviewedBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BookResponse> books = bookService.getMostReviewedBooks(pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(books);
    }

    /**
     * Get highest rated books (quality)
     * GET /api/books/highest-rated?minReviews=5&page=0&size=10
     */
    @GetMapping("/highest-rated")
    public ResponseEntity<Page<BookResponse>> getHighestRatedBooks(
            @RequestParam(defaultValue = "5") long minReviews,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BookResponse> books = bookService.getHighestRatedBooks(minReviews, pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(books);
    }

    /**
     * Get recently added books (new arrivals)
     * GET /api/books/recent?page=0&size=10
     */
    @GetMapping("/recent")
    public ResponseEntity<Page<BookResponse>> getRecentlyAddedBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BookResponse> books = bookService.getRecentlyAddedBooks(pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(books);
    }

    /**
     * Get trending books (most reviewed in last 30 days)
     * GET /api/books/trending?size=10
     */
    @GetMapping("/trending")
    public ResponseEntity<List<BookResponse>> getTrendingBooks(
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(0, size);
        List<BookResponse> books = bookService.getTrendingBooks(pageable).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(books);
    }

    /**
     * Verify book (admin only)
     * PUT /api/books/{id}/verify
     */
    @PutMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponse> verifyBook(@PathVariable Long id) {
        Book book = bookService.verifyBook(id);
        return ResponseEntity.ok(mapToResponse(book));
    }

    /**
     * Check if book exists by ISBN
     * GET /api/books/check-isbn/{isbn}
     */
    @GetMapping("/check-isbn/{isbn}")
    public ResponseEntity<Map<String, Boolean>> checkIsbnExists(@PathVariable String isbn) {
        boolean exists = bookService.existsByIsbn(isbn);

        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all books (paginated, admin only)
     * GET /api/books?page=0&size=20
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BookResponse>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<BookResponse> books = bookService.getAllBooks(pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(books);
    }

    // ============================================
    // HELPER METHOD: Map Entity to Response DTO
    // ============================================

    private BookResponse mapToResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .googleBooksId(book.getGoogleBooksId())
                .openLibraryId(book.getOpenLibraryId())
                .coverUrl(book.getCoverUrl())
                .description(book.getDescription())
                .publishedYear(book.getPublishedYear())
                .genre(book.getGenre())
                .pageCount(book.getPageCount())
                .averageRating(book.getAverageRating())
                .publisher(book.getPublisher())
                .language(book.getLanguage())
                .source(book.getSource())
                .isVerified(book.getIsVerified())
                .createdAt(book.getCreatedAt())
                .build();
    }
}