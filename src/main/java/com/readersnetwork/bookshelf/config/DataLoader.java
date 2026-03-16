package com.readersnetwork.bookshelf.config;

import com.readersnetwork.bookshelf.entity.Book;
import com.readersnetwork.bookshelf.entity.BookSource;
import com.readersnetwork.bookshelf.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final BookRepository bookRepository;

    @Override
    public void run(String... args) {
        if (bookRepository.count() > 0) {
            log.info("Books already loaded, skipping seed data.");
            return;
        }

        log.info("Loading demo book data...");

        List<Book> books = List.of(
                Book.builder()
                        .title("To Kill a Mockingbird")
                        .author("Harper Lee")
                        .isbn("9780061120084")
                        .description("The unforgettable novel of a childhood in a sleepy Southern town and the crisis of conscience that rocked it. A gripping, heart-wrenching, and wholly remarkable tale of coming-of-age in a South poisoned by virulent prejudice.")
                        .publishedYear(1960)
                        .genre("Fiction")
                        .pageCount(336)
                        .averageRating(4.27)
                        .publisher("Harper Perennial")
                        .language("en")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780061120084-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("1984")
                        .author("George Orwell")
                        .isbn("9780451524935")
                        .description("Among the seminal texts of the 20th century, Nineteen Eighty-Four is a rare work that grows more haunting as its dystopian purgatory becomes more real.")
                        .publishedYear(1949)
                        .genre("Dystopian Fiction")
                        .pageCount(328)
                        .averageRating(4.19)
                        .publisher("Signet Classics")
                        .language("en")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780451524935-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("The Great Gatsby")
                        .author("F. Scott Fitzgerald")
                        .isbn("9780743273565")
                        .description("The story of the mysteriously wealthy Jay Gatsby and his love for the beautiful Daisy Buchanan, of lavish parties on Long Island at a time when The New York Times noted 'ichol was cheap.'")
                        .publishedYear(1925)
                        .genre("Fiction")
                        .pageCount(180)
                        .averageRating(3.93)
                        .publisher("Scribner")
                        .language("en")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780743273565-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("One Hundred Years of Solitude")
                        .author("Gabriel Garcia Marquez")
                        .isbn("9780060883287")
                        .description("One of the 20th century's enduring works, One Hundred Years of Solitude is a widely beloved and acclaimed novel that tells the story of the Buendia family and their rise and fall in the mythical town of Macondo.")
                        .publishedYear(1967)
                        .genre("Magical Realism")
                        .pageCount(417)
                        .averageRating(4.08)
                        .publisher("Harper Perennial")
                        .language("es")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780060883287-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("Pride and Prejudice")
                        .author("Jane Austen")
                        .isbn("9780141439518")
                        .description("Since its immediate success in 1813, Pride and Prejudice has remained one of the most popular novels in the English language. It depicts life in genteel rural society at the turn of the 19th century.")
                        .publishedYear(1813)
                        .genre("Romance")
                        .pageCount(432)
                        .averageRating(4.28)
                        .publisher("Penguin Classics")
                        .language("en")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780141439518-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("The Hobbit")
                        .author("J.R.R. Tolkien")
                        .isbn("9780547928227")
                        .description("Bilbo Baggins is a hobbit who enjoys a comfortable, unambitious life, rarely traveling any farther than his pantry or cellar. But his contentment is disturbed when the wizard Gandalf and a company of dwarves arrive on his doorstep.")
                        .publishedYear(1937)
                        .genre("Fantasy")
                        .pageCount(310)
                        .averageRating(4.28)
                        .publisher("Mariner Books")
                        .language("en")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780547928227-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("Harry Potter and the Sorcerer's Stone")
                        .author("J.K. Rowling")
                        .isbn("9780590353427")
                        .description("Harry Potter has never even heard of Hogwarts when the letters start dropping on the doormat. Rescued by an enormous man with a long, wild beard, Harry is told an incredible truth: he is a wizard.")
                        .publishedYear(1997)
                        .genre("Fantasy")
                        .pageCount(309)
                        .averageRating(4.47)
                        .publisher("Scholastic")
                        .language("en")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780590353427-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("The Catcher in the Rye")
                        .author("J.D. Salinger")
                        .isbn("9780316769488")
                        .description("The hero-narrator of The Catcher in the Rye is an ancient child of sixteen, a native New Yorker named Holden Caulfield. Through circumstances that tend to preclude adult, responsible action, Holden retreats from the world.")
                        .publishedYear(1951)
                        .genre("Fiction")
                        .pageCount(277)
                        .averageRating(3.80)
                        .publisher("Little, Brown and Company")
                        .language("en")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780316769488-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("Brave New World")
                        .author("Aldous Huxley")
                        .isbn("9780060850524")
                        .description("Aldous Huxley's profoundly important classic of world literature, Brave New World is a searching vision of an unequal, technologically-advanced future where humans are genetically bred, socially indoctrinated, and pharmaceutically anesthetized.")
                        .publishedYear(1932)
                        .genre("Dystopian Fiction")
                        .pageCount(288)
                        .averageRating(3.99)
                        .publisher("Harper Perennial")
                        .language("en")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780060850524-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("The Lord of the Rings")
                        .author("J.R.R. Tolkien")
                        .isbn("9780618640157")
                        .description("In ancient times the Rings of Power were crafted by the Elven-smiths, and Sauron, the Dark Lord, forged the One Ring, filling it with his own power so that he could rule all others.")
                        .publishedYear(1954)
                        .genre("Fantasy")
                        .pageCount(1178)
                        .averageRating(4.52)
                        .publisher("Mariner Books")
                        .language("en")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780618640157-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("Don Quixote")
                        .author("Miguel de Cervantes")
                        .isbn("9780060934347")
                        .description("The story of the gentle knight and his faithful squire has been translated into more languages than any other book except the Bible. Widely regarded as the first modern novel, it remains the most heartwarming.")
                        .publishedYear(1605)
                        .genre("Fiction")
                        .pageCount(982)
                        .averageRating(3.88)
                        .publisher("Ecco")
                        .language("es")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780060934347-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("Dune")
                        .author("Frank Herbert")
                        .isbn("9780441013593")
                        .description("Set on the desert planet Arrakis, Dune is the story of the boy Paul Atreides, heir to a noble family tasked with ruling an inhospitable world where the only thing of value is a spice capable of extending life and enhancing consciousness.")
                        .publishedYear(1965)
                        .genre("Science Fiction")
                        .pageCount(688)
                        .averageRating(4.25)
                        .publisher("Ace Books")
                        .language("en")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780441013593-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("The Alchemist")
                        .author("Paulo Coelho")
                        .isbn("9780062315007")
                        .description("Paulo Coelho's masterwork, an inspiring tale of self-discovery. Santiago, an Andalusian shepherd boy, travels from his homeland in Spain to the Egyptian desert in search of a treasure buried near the Pyramids.")
                        .publishedYear(1988)
                        .genre("Fiction")
                        .pageCount(197)
                        .averageRating(3.90)
                        .publisher("HarperOne")
                        .language("pt")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780062315007-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("Crime and Punishment")
                        .author("Fyodor Dostoevsky")
                        .isbn("9780143058144")
                        .description("Raskolnikov, a destitute and desperate former student, wanders through the slums of St Petersburg and commits a random murder without remorse or regret. He imagines himself to be a great man, above the law.")
                        .publishedYear(1866)
                        .genre("Psychological Fiction")
                        .pageCount(671)
                        .averageRating(4.24)
                        .publisher("Penguin Classics")
                        .language("ru")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780143058144-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build(),

                Book.builder()
                        .title("The Little Prince")
                        .author("Antoine de Saint-Exupery")
                        .isbn("9780156012195")
                        .description("A pilot stranded in the desert meets a young prince fallen to Earth from a tiny asteroid. The story is philosophical and includes social criticism, remarking on the strangeness of the adult world.")
                        .publishedYear(1943)
                        .genre("Fiction")
                        .pageCount(96)
                        .averageRating(4.32)
                        .publisher("Mariner Books")
                        .language("fr")
                        .coverUrl("https://covers.openlibrary.org/b/isbn/9780156012195-L.jpg")
                        .source(BookSource.MANUAL_ENTRY)
                        .build()
        );

        bookRepository.saveAll(books);
        log.info("Loaded {} demo books into the database.", books.size());
    }
}
