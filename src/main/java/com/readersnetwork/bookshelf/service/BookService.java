package com.readersnetwork.bookshelf.service;

import com.readersnetwork.bookshelf.entity.Book;
import com.readersnetwork.bookshelf.entity.BookSource;
import com.readersnetwork.bookshelf.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * MAIN METHOD: Search for a book
     * User provides: "Harry Potter" or "1984 George Orwell"
     * 
     * Flow:
     * 1. Search our database first (fast!)
     * 2. If not found → Query Google Books API
     * 3. Save to database
     * 4. Return book
     */
    @Transactional
    public Book searchAndCreateBook(String searchQuery) {
        // Step 1: Try to find in our database first
        Page<Book> existingBooks = bookRepository.searchBooks(
                searchQuery,
                PageRequest.of(0, 1) // Get first result only
        );

        if (!existingBooks.isEmpty()) {
            // Found it! Return immediately (no API call needed)
            return existingBooks.getContent().get(0);
        }

        // Step 2: Not in database → Query Google Books API
        Book bookFromApi = fetchFromGoogleBooks(searchQuery);

        if (bookFromApi == null) {
            throw new RuntimeException("Book not found: " + searchQuery);
        }

        // Step 3: Before saving, check if another user just added this book
        // (Race condition protection using the ISBN/Google ID we got from API)
        Optional<Book> duplicate = bookRepository.findByAnyIdentifier(
                bookFromApi.getIsbn(), // Now we have ISBN (from API)
                bookFromApi.getGoogleBooksId(), // Now we have Google ID (from API)
                null, // OpenLibrary ID (future feature)
                bookFromApi.getTitle(),
                bookFromApi.getAuthor());

        if (duplicate.isPresent()) {
            return duplicate.get(); // Another user just added it!
        }

        // Step 4: Save and return
        return bookRepository.save(bookFromApi);
    }

    /**
     * Get multiple search results (for search results page)
     * This searches ONLY in our database (fast)
     * 
     * Use this when showing: "Search results for 'Harry Potter'"
     */
    public Page<Book> searchBooks(String query, Pageable pageable) {
        return bookRepository.searchBooks(query, pageable);
    }

    /**
     * Fetch book data from Google Books API
     * Input: User's search query (e.g., "1984 George Orwell")
     * Output: Book entity with ISBN, Google Books ID, etc. filled in
     */
    private Book fetchFromGoogleBooks(String searchQuery) {
        try {
            // Build API URL
            String url = "https://www.googleapis.com/books/v1/volumes?q=" + searchQuery;

            GoogleBooksResponse response = restTemplate.getForObject(url, GoogleBooksResponse.class);

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                return null; // Book not found in Google Books
            }

            // Get first result
            GoogleBooksItem item = response.getItems().get(0);
            VolumeInfo info = item.getVolumeInfo();

            // Map API response to our Book entity
            Book book = new Book();
            book.setTitle(info.getTitle());
            book.setAuthor(info.getAuthors() != null && !info.getAuthors().isEmpty()
                    ? String.join(", ", info.getAuthors())
                    : "Unknown Author");

            // THESE come from the API response (not from user!)
            book.setIsbn(extractIsbn(info.getIndustryIdentifiers()));
            book.setGoogleBooksId(item.getId());

            book.setDescription(info.getDescription());
            book.setCoverUrl(info.getImageLinks() != null ? info.getImageLinks().getThumbnail() : null);

            // Extract year from publishedDate (e.g., "2021-05-10" → 2021)
            if (info.getPublishedDate() != null && info.getPublishedDate().length() >= 4) {
                try {
                    book.setPublishedYear(Integer.parseInt(info.getPublishedDate().substring(0, 4)));
                } catch (NumberFormatException e) {
                    book.setPublishedYear(null);
                }
            }

            book.setPageCount(info.getPageCount());
            book.setGenre(extractGenre(info.getCategories()));
            book.setAverageRating(0.0); // Will be calculated from reviews later
            book.setSource(BookSource.GOOGLE_BOOKS);

            return book;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extract ISBN from Google Books API response
     */
    private String extractIsbn(List<IndustryIdentifier> identifiers) {
        if (identifiers == null)
            return null;

        // Prefer ISBN_13 (most common)
        for (IndustryIdentifier id : identifiers) {
            if ("ISBN_13".equals(id.getType())) {
                return id.getIdentifier();
            }
        }

        // Fall back to ISBN_10
        for (IndustryIdentifier id : identifiers) {
            if ("ISBN_10".equals(id.getType())) {
                return id.getIdentifier();
            }
        }

        return null; // No ISBN found
    }

    /**
     * Extract first genre/category from Google Books
     */
    private String extractGenre(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return categories.get(0);
    }

    // ============================================
    // OTHER USEFUL METHODS
    // ============================================

    /**
     * Get book by ID (for book details page)
     */
    public Book getBookById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Book ID cannot be null");
        }
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
    }

    /**
     * Advanced search with filters
     * Use this for: "Show me Fantasy books from 2020"
     */
    public Page<Book> advancedSearch(String title, String author, String genre, Integer year, Pageable pageable) {
        return bookRepository.advancedSearch(title, author, genre, year, pageable);
    }

    /**
     * Get books by genre
     */
    public Page<Book> getBooksByGenre(String genre, Pageable pageable) {
        return bookRepository.findByGenre(genre, pageable);
    }

    /**
     * Get most reviewed books (popular books)
     */
    public Page<Book> getMostReviewedBooks(Pageable pageable) {
        return bookRepository.findMostReviewedBooks(pageable);
    }

    /**
     * Get highest rated books (quality books with at least X reviews)
     */
    public Page<Book> getHighestRatedBooks(long minReviews, Pageable pageable) {
        return bookRepository.findHighestRatedBooks(minReviews, pageable);
    }

    /**
     * Get recently added books (new arrivals)
     */
    public Page<Book> getRecentlyAddedBooks(Pageable pageable) {
        return bookRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Get trending books (most reviewed in last 30 days)
     */
    public List<Book> getTrendingBooks(Pageable pageable) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return bookRepository.findTrendingBooks(thirtyDaysAgo, pageable);
    }

    /**
     * Check if book exists by ISBN
     */
    public boolean existsByIsbn(String isbn) {
        return bookRepository.existsByIsbn(isbn);
    }

    // ============================================
    // GOOGLE BOOKS API RESPONSE CLASSES
    // These map the JSON response from Google Books
    // ============================================

    static class GoogleBooksResponse {
        private List<GoogleBooksItem> items;

        public List<GoogleBooksItem> getItems() {
            return items;
        }

        public void setItems(List<GoogleBooksItem> items) {
            this.items = items;
        }
    }

    static class GoogleBooksItem {
        private String id;
        private VolumeInfo volumeInfo;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public VolumeInfo getVolumeInfo() {
            return volumeInfo;
        }

        public void setVolumeInfo(VolumeInfo volumeInfo) {
            this.volumeInfo = volumeInfo;
        }
    }

    static class VolumeInfo {
        private String title;
        private List<String> authors;
        private String description;
        private List<IndustryIdentifier> industryIdentifiers;
        private ImageLinks imageLinks;
        private String publishedDate;
        private Integer pageCount;
        private List<String> categories;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<String> getAuthors() {
            return authors;
        }

        public void setAuthors(List<String> authors) {
            this.authors = authors;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<IndustryIdentifier> getIndustryIdentifiers() {
            return industryIdentifiers;
        }

        public void setIndustryIdentifiers(List<IndustryIdentifier> industryIdentifiers) {
            this.industryIdentifiers = industryIdentifiers;
        }

        public ImageLinks getImageLinks() {
            return imageLinks;
        }

        public void setImageLinks(ImageLinks imageLinks) {
            this.imageLinks = imageLinks;
        }

        public String getPublishedDate() {
            return publishedDate;
        }

        public void setPublishedDate(String publishedDate) {
            this.publishedDate = publishedDate;
        }

        public Integer getPageCount() {
            return pageCount;
        }

        public void setPageCount(Integer pageCount) {
            this.pageCount = pageCount;
        }

        public List<String> getCategories() {
            return categories;
        }

        public void setCategories(List<String> categories) {
            this.categories = categories;
        }
    }

    static class IndustryIdentifier {
        private String type;
        private String identifier;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }
    }

    static class ImageLinks {
        private String thumbnail;

        public String getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
        }
    }
}