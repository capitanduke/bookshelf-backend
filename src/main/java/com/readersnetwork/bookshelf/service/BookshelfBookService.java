package com.readersnetwork.bookshelf.service;

import com.readersnetwork.bookshelf.dto.request.BookshelfBookRequest;
import com.readersnetwork.bookshelf.dto.response.BookResponse;
import com.readersnetwork.bookshelf.dto.response.BookshelfBookResponse;
import com.readersnetwork.bookshelf.dto.response.BookshelfResponse;
import com.readersnetwork.bookshelf.entity.Book;
import com.readersnetwork.bookshelf.entity.Bookshelf;
import com.readersnetwork.bookshelf.entity.BookshelfBook;
import com.readersnetwork.bookshelf.entity.PrivacyLevel;
import com.readersnetwork.bookshelf.repository.BookRepository;
import com.readersnetwork.bookshelf.repository.BookshelfBookRepository;
import com.readersnetwork.bookshelf.repository.BookshelfRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@SuppressWarnings("null")
public class BookshelfBookService {

    private final BookshelfBookRepository bookshelfBookRepository;
    private final BookshelfRepository bookshelfRepository;
    private final BookRepository bookRepository;

    public BookshelfBookResponse addBookToBookshelf(Long bookshelfId, BookshelfBookRequest request,
            Long currentUserId) {
        if (bookshelfId == null) {
            throw new IllegalArgumentException("Bookshelf ID cannot be null");
        }

        Long bookId = request.getBookId();
        if (bookId == null) {
            throw new IllegalArgumentException("Book ID cannot be null");
        }

        log.debug("Adding book {} to bookshelf {} by user {}", bookId, bookshelfId, currentUserId);

        Bookshelf bookshelf = bookshelfRepository.findById(bookshelfId)
                .orElseThrow(() -> new RuntimeException("Bookshelf not found with id: " + bookshelfId));

        if (!bookshelf.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("You can only add books to your own bookshelves");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));

        if (bookshelfBookRepository.existsByBookshelfIdAndBookId(bookshelfId, bookId)) {
            throw new RuntimeException("Book is already in this bookshelf");
        }

        Integer position = request.getPosition();
        if (position == null) {
            long currentCount = bookshelfBookRepository.countByBookshelfId(bookshelfId);
            position = (int) currentCount;
        }

        BookshelfBook bookshelfBook = BookshelfBook.builder()
                .bookshelf(bookshelf)
                .book(book)
                .position(position)
                .build();

        BookshelfBook saved = bookshelfBookRepository.save(bookshelfBook);

        log.info("Book {} added to bookshelf {} successfully", request.getBookId(), bookshelfId);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<BookshelfBookResponse> getBooksInBookshelf(Long bookshelfId, Long currentUserId, Pageable pageable) {
        log.debug("Fetching books in bookshelf {} for user {}", bookshelfId, currentUserId);

        Bookshelf bookshelf = bookshelfRepository.findById(bookshelfId)
                .orElseThrow(() -> new RuntimeException("Bookshelf not found with id: " + bookshelfId));

        if (!canAccessBookshelf(bookshelf, currentUserId)) {
            throw new RuntimeException("You don't have permission to view this bookshelf");
        }

        Page<BookshelfBook> bookshelfBooks = bookshelfBookRepository.findByBookshelfId(bookshelfId, pageable);
        return bookshelfBooks.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<BookshelfBookResponse> getBooksInBookshelfOrdered(Long bookshelfId, Long currentUserId) {
        log.debug("Fetching ordered books in bookshelf {} for user {}", bookshelfId, currentUserId);

        Bookshelf bookshelf = bookshelfRepository.findById(bookshelfId)
                .orElseThrow(() -> new RuntimeException("Bookshelf not found with id: " + bookshelfId));

        if (!canAccessBookshelf(bookshelf, currentUserId)) {
            throw new RuntimeException("You don't have permission to view this bookshelf");
        }

        List<BookshelfBook> bookshelfBooks = bookshelfBookRepository.findByBookshelfIdOrderByPositionAsc(bookshelfId);
        return bookshelfBooks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void removeBookFromBookshelf(Long bookshelfId, Long bookId, Long currentUserId) {
        log.debug("Removing book {} from bookshelf {} by user {}", bookId, bookshelfId, currentUserId);

        Bookshelf bookshelf = bookshelfRepository.findById(bookshelfId)
                .orElseThrow(() -> new RuntimeException("Bookshelf not found with id: " + bookshelfId));

        if (!bookshelf.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("You can only remove books from your own bookshelves");
        }

        BookshelfBook bookshelfBook = bookshelfBookRepository.findByBookshelfIdAndBookId(bookshelfId, bookId)
                .orElseThrow(() -> new RuntimeException("Book not found in this bookshelf"));

        bookshelfBookRepository.delete(bookshelfBook);

        log.info("Book {} removed from bookshelf {} successfully", bookId, bookshelfId);
    }

    public BookshelfBookResponse updateBookPosition(Long bookshelfId, Long bookId, Integer newPosition,
            Long currentUserId) {
        log.debug("Updating position of book {} in bookshelf {} to position {}", bookId, bookshelfId, newPosition);

        Bookshelf bookshelf = bookshelfRepository.findById(bookshelfId)
                .orElseThrow(() -> new RuntimeException("Bookshelf not found with id: " + bookshelfId));

        if (!bookshelf.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("You can only modify your own bookshelves");
        }

        BookshelfBook bookshelfBook = bookshelfBookRepository.findByBookshelfIdAndBookId(bookshelfId, bookId)
                .orElseThrow(() -> new RuntimeException("Book not found in this bookshelf"));

        bookshelfBook.setPosition(newPosition);
        BookshelfBook updated = bookshelfBookRepository.save(bookshelfBook);

        log.info("Book {} position updated in bookshelf {}", bookId, bookshelfId);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public boolean isBookInBookshelf(Long bookshelfId, Long bookId) {
        return bookshelfBookRepository.existsByBookshelfIdAndBookId(bookshelfId, bookId);
    }

    @Transactional(readOnly = true)
    public List<BookshelfResponse> getBookshelvesContainingBook(Long bookId, Long currentUserId) {
        log.debug("Fetching bookshelves containing book {} for user {}", bookId, currentUserId);

        if (!bookRepository.existsById(bookId)) {
            throw new RuntimeException("Book not found with id: " + bookId);
        }

        List<Bookshelf> bookshelves = bookshelfBookRepository.findBookshelvesContainingBook(bookId);

        return bookshelves.stream()
                .filter(shelf -> canAccessBookshelf(shelf, currentUserId))
                .map(this::mapToBookshelfResponse)
                .collect(Collectors.toList());
    }

    private boolean canAccessBookshelf(Bookshelf bookshelf, Long currentUserId) {
        if (bookshelf.getUser().getId().equals(currentUserId)) {
            return true;
        }
        return bookshelf.getPrivacy() == PrivacyLevel.PUBLIC;
    }

    private BookshelfBookResponse mapToResponse(BookshelfBook bookshelfBook) {
        return BookshelfBookResponse.builder()
                .id(bookshelfBook.getId())
                .bookshelfId(bookshelfBook.getBookshelf().getId())
                .bookshelfName(bookshelfBook.getBookshelf().getName())
                .book(mapToBookResponse(bookshelfBook.getBook()))
                .position(bookshelfBook.getPosition())
                .addedAt(bookshelfBook.getAddedAt())
                .build();
    }

    private BookResponse mapToBookResponse(Book book) {
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

    private BookshelfResponse mapToBookshelfResponse(Bookshelf bookshelf) {
        long bookCount = bookshelfBookRepository.countByBookshelfId(bookshelf.getId());

        return BookshelfResponse.builder()
                .id(bookshelf.getId())
                .userId(bookshelf.getUser().getId())
                .username(bookshelf.getUser().getUsername())
                .name(bookshelf.getName())
                .description(bookshelf.getDescription())
                .privacy(bookshelf.getPrivacy())
                .bookCount((int) bookCount)
                .createdAt(bookshelf.getCreatedAt())
                .updatedAt(bookshelf.getUpdatedAt())
                .build();
    }
}