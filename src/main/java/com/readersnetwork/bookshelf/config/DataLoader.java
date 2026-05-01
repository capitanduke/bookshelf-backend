package com.readersnetwork.bookshelf.config;

import com.readersnetwork.bookshelf.entity.Book;
import com.readersnetwork.bookshelf.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the book catalog on first startup by fetching books from the Google Books API.
 *
 * Strategy:
 * - Runs asynchronously in a background thread so the app starts immediately
 * - Skips seeding entirely if the DB already has books (restart-safe)
 * - Skips seeding if disabled via config or if no API key is configured
 * - Iterates 12 subject queries × 25 pages × 40 results = ~10,000 raw results
 * - Deduplicates each book by googleBooksId / ISBN / title+author before saving
 * - 200ms delay between API calls to respect rate limits
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final GoogleBooksApiClient googleBooksApiClient;

    @Value("${bookshelf.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${bookshelf.seed.delay-ms:200}")
    private long delayMs;

    /**
     * Subject queries used to seed the catalog.
     * Each query is paginated up to 25 pages × 40 results = 1,000 books per subject.
     * 12 subjects × 1,000 = ~12,000 raw results; expect ~8,000–10,000 after deduplication.
     */
    private static final List<String> SEED_QUERIES = List.of(
            "subject:fiction",
            "subject:science+fiction",
            "subject:fantasy",
            "subject:mystery",
            "subject:thriller",
            "subject:romance",
            "subject:biography",
            "subject:history",
            "subject:horror",
            "subject:self-help",
            "subject:classics",
            "subject:nonfiction"
    );

    private static final int MAX_RESULTS_PER_PAGE = 40;
    private static final int PAGES_PER_QUERY = 25; // 25 × 40 = 1,000 per subject

    @Override
    public void run(String... args) {
        if (bookRepository.count() > 0) {
            log.info("Book catalog already populated ({} books). Skipping seed.", bookRepository.count());
            return;
        }

        if (!seedEnabled) {
            log.info("Book seeding is disabled (bookshelf.seed.enabled=false). Skipping.");
            return;
        }

        if (!googleBooksApiClient.hasApiKey()) {
            log.warn("No Google Books API key configured. Skipping catalog seeding. " +
                     "Set GOOGLE_BOOKS_API_KEY environment variable to enable seeding.");
            return;
        }

        log.info("Starting async book catalog seeding from Google Books API...");
        log.info("Queries: {}, Pages per query: {}, Max results per page: {}",
                SEED_QUERIES.size(), PAGES_PER_QUERY, MAX_RESULTS_PER_PAGE);
        log.info("Estimated API calls: {} | Estimated time: ~{} seconds",
                SEED_QUERIES.size() * PAGES_PER_QUERY,
                (SEED_QUERIES.size() * PAGES_PER_QUERY * delayMs) / 1000);

        Thread seedThread = new Thread(this::runSeeding, "book-catalog-seeder");
        seedThread.setDaemon(true);
        seedThread.start();
    }

    private void runSeeding() {
        int totalSaved = 0;
        int totalSkipped = 0;

        for (String query : SEED_QUERIES) {
            log.info("[Seeder] Starting query: '{}' ...", query);
            int savedForQuery = 0;

            for (int page = 0; page < PAGES_PER_QUERY; page++) {
                int startIndex = page * MAX_RESULTS_PER_PAGE;

                try {
                    List<Book> books = googleBooksApiClient.fetchBooks(query, MAX_RESULTS_PER_PAGE, startIndex);

                    if (books.isEmpty()) {
                        log.debug("[Seeder] No results for query='{}' page={}. Moving to next query.", query, page);
                        break; // Google has no more results for this query
                    }

                    for (Book book : books) {
                        try {
                            boolean isDuplicate = bookRepository.findByAnyIdentifier(
                                    book.getIsbn(),
                                    book.getGoogleBooksId(),
                                    null,
                                    book.getTitle(),
                                    book.getAuthor()
                            ).isPresent();

                            if (isDuplicate) {
                                totalSkipped++;
                            } else {
                                bookRepository.save(book);
                                savedForQuery++;
                                totalSaved++;
                            }
                        } catch (Exception e) {
                            log.debug("[Seeder] Failed to save book '{}': {}", book.getTitle(), e.getMessage());
                            totalSkipped++;
                        }
                    }

                    Thread.sleep(delayMs);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[Seeder] Seeding interrupted. Saved {} books before interruption.", totalSaved);
                    return;
                } catch (Exception e) {
                    log.warn("[Seeder] Error on query='{}' page={}: {}. Continuing...", query, page, e.getMessage());
                }
            }

            log.info("[Seeder] Finished query: '{}' — saved {} books (total so far: {})",
                    query, savedForQuery, totalSaved);
        }

        log.info("=================================================");
        log.info("[Seeder] Catalog seeding complete!");
        log.info("[Seeder] Books saved:   {}", totalSaved);
        log.info("[Seeder] Books skipped: {} (duplicates or errors)", totalSkipped);
        log.info("[Seeder] Total in DB:   {}", bookRepository.count());
        log.info("=================================================");
    }
}
