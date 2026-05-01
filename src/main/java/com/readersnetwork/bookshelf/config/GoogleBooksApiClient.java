package com.readersnetwork.bookshelf.config;

import com.readersnetwork.bookshelf.entity.Book;
import com.readersnetwork.bookshelf.entity.BookSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for the Google Books API.
 *
 * Responsibilities:
 * - Build API URLs (with or without API key)
 * - Fetch a single book by search query (used by BookService for user searches)
 * - Fetch multiple books with pagination (used by DataLoader for seeding)
 * - Map API JSON responses to Book entities
 */
@Component
@Slf4j
public class GoogleBooksApiClient {

    private static final String BASE_URL = "https://www.googleapis.com/books/v1/volumes";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${google.books.api.key:}")
    private String apiKey;

    // ============================================
    // PUBLIC API
    // ============================================

    /**
     * Fetch the first matching book for a search query.
     * Used by BookService when a user searches for a book not yet in the DB.
     *
     * @param query search query (e.g. "1984 George Orwell")
     * @return Book entity or null if not found
     */
    public Book fetchFirstBook(String query) {
        List<Book> results = fetchBooks(query, 1, 0);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Fetch multiple books with pagination support.
     * Used by DataLoader to bulk-seed the database.
     *
     * @param query      search query (e.g. "subject:fiction")
     * @param maxResults number of results to request (max 40, Google's hard limit)
     * @param startIndex pagination offset (0, 40, 80, ...)
     * @return list of Book entities (may be empty if API returns nothing)
     */
    public List<Book> fetchBooks(String query, int maxResults, int startIndex) {
        List<Book> books = new ArrayList<>();

        try {
            String url = buildUrl(query, maxResults, startIndex);
            log.debug("Calling Google Books API: {}", url);

            GoogleBooksResponse response = restTemplate.getForObject(url, GoogleBooksResponse.class);

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                return books;
            }

            for (GoogleBooksItem item : response.getItems()) {
                Book book = mapToBook(item);
                if (book != null) {
                    books.add(book);
                }
            }

        } catch (Exception e) {
            log.warn("Google Books API call failed for query='{}' startIndex={}: {}", query, startIndex, e.getMessage());
        }

        return books;
    }

    /**
     * Returns true if the API key is configured, false otherwise.
     * Used by DataLoader to decide whether seeding can proceed.
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    private String buildUrl(String query, int maxResults, int startIndex) {
        String url = BASE_URL
                + "?q=" + query
                + "&maxResults=" + Math.min(maxResults, 40)
                + "&startIndex=" + startIndex
                + "&printType=books";

        if (hasApiKey()) {
            url += "&key=" + apiKey;
        }

        return url;
    }

    /**
     * Maps a single GoogleBooksItem to a Book entity.
     * Returns null if the item is missing required fields (title or author).
     */
    private Book mapToBook(GoogleBooksItem item) {
        if (item == null || item.getVolumeInfo() == null) {
            return null;
        }

        VolumeInfo info = item.getVolumeInfo();

        if (info.getTitle() == null || info.getTitle().isBlank()) {
            return null;
        }

        String author = (info.getAuthors() != null && !info.getAuthors().isEmpty())
                ? String.join(", ", info.getAuthors())
                : "Unknown Author";

        Book book = new Book();
        book.setTitle(info.getTitle().trim());
        book.setAuthor(author);
        book.setGoogleBooksId(item.getId());
        book.setIsbn(extractIsbn(info.getIndustryIdentifiers()));
        book.setDescription(info.getDescription());
        book.setCoverUrl(info.getImageLinks() != null ? info.getImageLinks().getThumbnail() : null);
        book.setPageCount(info.getPageCount());
        book.setGenre(extractGenre(info.getCategories()));
        book.setPublisher(info.getPublisher());
        book.setLanguage(info.getLanguage());
        book.setAverageRating(0.0);
        book.setSource(BookSource.GOOGLE_BOOKS);
        book.setIsVerified(true);

        if (info.getPublishedDate() != null && info.getPublishedDate().length() >= 4) {
            try {
                book.setPublishedYear(Integer.parseInt(info.getPublishedDate().substring(0, 4)));
            } catch (NumberFormatException e) {
                book.setPublishedYear(null);
            }
        }

        return book;
    }

    private String extractIsbn(List<IndustryIdentifier> identifiers) {
        if (identifiers == null) return null;

        for (IndustryIdentifier id : identifiers) {
            if ("ISBN_13".equals(id.getType())) return id.getIdentifier();
        }
        for (IndustryIdentifier id : identifiers) {
            if ("ISBN_10".equals(id.getType())) return id.getIdentifier();
        }

        return null;
    }

    private String extractGenre(List<String> categories) {
        if (categories == null || categories.isEmpty()) return null;
        return categories.get(0);
    }

    // ============================================
    // GOOGLE BOOKS API RESPONSE CLASSES
    // ============================================

    static class GoogleBooksResponse {
        private List<GoogleBooksItem> items;

        public List<GoogleBooksItem> getItems() { return items; }
        public void setItems(List<GoogleBooksItem> items) { this.items = items; }
    }

    static class GoogleBooksItem {
        private String id;
        private VolumeInfo volumeInfo;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public VolumeInfo getVolumeInfo() { return volumeInfo; }
        public void setVolumeInfo(VolumeInfo volumeInfo) { this.volumeInfo = volumeInfo; }
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
        private String publisher;
        private String language;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<String> getAuthors() { return authors; }
        public void setAuthors(List<String> authors) { this.authors = authors; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<IndustryIdentifier> getIndustryIdentifiers() { return industryIdentifiers; }
        public void setIndustryIdentifiers(List<IndustryIdentifier> identifiers) { this.industryIdentifiers = identifiers; }
        public ImageLinks getImageLinks() { return imageLinks; }
        public void setImageLinks(ImageLinks imageLinks) { this.imageLinks = imageLinks; }
        public String getPublishedDate() { return publishedDate; }
        public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }
        public Integer getPageCount() { return pageCount; }
        public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        public String getPublisher() { return publisher; }
        public void setPublisher(String publisher) { this.publisher = publisher; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    static class IndustryIdentifier {
        private String type;
        private String identifier;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
    }

    static class ImageLinks {
        private String thumbnail;

        public String getThumbnail() { return thumbnail; }
        public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    }
}
