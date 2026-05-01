package com.readersnetwork.bookshelf.service;

import com.readersnetwork.bookshelf.config.GoogleBooksApiClient;
import com.readersnetwork.bookshelf.entity.Book;
import com.readersnetwork.bookshelf.entity.BookSource;
import com.readersnetwork.bookshelf.exception.BookNotFoundException;
import com.readersnetwork.bookshelf.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GoogleBooksApiClient googleBooksApiClient;

    // ============================================
    // SEARCH & API INTEGRATION
    // ============================================

    /**
     * MAIN METHOD: Search for a book.
     * User provides: "Harry Potter" or "1984 George Orwell"
     *
     * Flow:
     * 1. Search our database first (fast)
     * 2. If not found → query Google Books API via GoogleBooksApiClient
     * 3. Deduplicate against race conditions
     * 4. Save and return
     */
    @Transactional
    public Book searchAndCreateBook(String searchQuery) {
        // Step 1: Try to find in our database first
        Page<Book> existingBooks = bookRepository.searchBooks(
                searchQuery,
                PageRequest.of(0, 1)
        );

        if (!existingBooks.isEmpty()) {
            return existingBooks.getContent().get(0);
        }

        // Step 2: Not in database → query Google Books API
        Book bookFromApi = googleBooksApiClient.fetchFirstBook(searchQuery);

        if (bookFromApi == null) {
            throw new BookNotFoundException("Book not found: " + searchQuery);
        }

        // Step 3: Race condition protection — another user may have just added it
        Optional<Book> duplicate = bookRepository.findByAnyIdentifier(
                bookFromApi.getIsbn(),
                bookFromApi.getGoogleBooksId(),
                null,
                bookFromApi.getTitle(),
                bookFromApi.getAuthor());

        if (duplicate.isPresent()) {
            return duplicate.get();
        }

        // Step 4: Save and return
        return bookRepository.save(bookFromApi);
    }

    /**
     * Get multiple search results (for search results page).
     * Searches only in our database.
     */
    public Page<Book> searchBooks(String query, Pageable pageable) {
        return bookRepository.searchBooks(query, pageable);
    }

    /**
     * Advanced search with filters.
     */
    public Page<Book> advancedSearch(String title, String author, String genre, Integer year, Pageable pageable) {
        return bookRepository.advancedSearch(title, author, genre, year, pageable);
    }

    // ============================================
    // MANUAL BOOK CREATION
    // ============================================

    /**
     * Create a book manually (when user can't find it in APIs).
     * Checks for duplicates before saving. Sets source=MANUAL_ENTRY, isVerified=false.
     */
    @Transactional
    public Book createBookManually(Book book) {
        Optional<Book> duplicate = bookRepository.findByAnyIdentifier(
                book.getIsbn(),
                book.getGoogleBooksId(),
                book.getOpenLibraryId(),
                book.getTitle(),
                book.getAuthor());

        if (duplicate.isPresent()) {
            throw new RuntimeException("Book already exists in database with ID: " + duplicate.get().getId());
        }

        if (book.getSource() == null) {
            book.setSource(BookSource.MANUAL_ENTRY);
        }
        if (book.getAverageRating() == null) {
            book.setAverageRating(0.0);
        }
        if (book.getIsVerified() == null) {
            book.setIsVerified(false);
        }

        return bookRepository.save(book);
    }

    /**
     * Update an existing book.
     */
    @Transactional
    public Book updateBook(Book book) {
        if (book.getId() == null) {
            throw new IllegalArgumentException("Book ID cannot be null for update");
        }
        if (!bookRepository.existsById(book.getId())) {
            throw new BookNotFoundException("Book not found with id: " + book.getId());
        }
        return bookRepository.save(book);
    }

    /**
     * Delete a book (admin only). Cascades to reviews, user books, etc.
     */
    @Transactional
    public void deleteBook(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Book ID cannot be null");
        }
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
        bookRepository.delete(book);
    }

    /**
     * Mark a book as verified (admin only).
     */
    @Transactional
    public Book verifyBook(Long id) {
        Book book = getBookById(id);
        book.setIsVerified(true);
        return bookRepository.save(book);
    }

    /**
     * Get all books paginated (admin panel).
     */
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    // ============================================
    // BASIC CRUD OPERATIONS
    // ============================================

    public Book getBookById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Book ID cannot be null");
        }
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
    }

    public boolean existsByIsbn(String isbn) {
        return bookRepository.existsByIsbn(isbn);
    }

    // ============================================
    // DISCOVERY & RECOMMENDATIONS
    // ============================================

    public Page<Book> getBooksByGenre(String genre, Pageable pageable) {
        return bookRepository.findByGenre(genre, pageable);
    }

    public Page<Book> getMostReviewedBooks(Pageable pageable) {
        return bookRepository.findMostReviewedBooks(pageable);
    }

    public Page<Book> getHighestRatedBooks(long minReviews, Pageable pageable) {
        return bookRepository.findHighestRatedBooks(minReviews, pageable);
    }

    public Page<Book> getRecentlyAddedBooks(Pageable pageable) {
        return bookRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<Book> getTrendingBooks(Pageable pageable) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return bookRepository.findTrendingBooks(thirtyDaysAgo, pageable);
    }
}
