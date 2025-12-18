package com.readersnetwork.bookshelf.service;

import com.readersnetwork.bookshelf.dto.request.BookshelfRequest;
import com.readersnetwork.bookshelf.dto.response.BookshelfResponse;
import com.readersnetwork.bookshelf.entity.Bookshelf;
import com.readersnetwork.bookshelf.entity.PrivacyLevel;
import com.readersnetwork.bookshelf.entity.User;
import com.readersnetwork.bookshelf.exception.BookshelfNotFoundException;
import com.readersnetwork.bookshelf.exception.UnauthorizedAccessException;
import com.readersnetwork.bookshelf.exception.UserNotFoundException;
import com.readersnetwork.bookshelf.repository.BookshelfRepository;
import com.readersnetwork.bookshelf.repository.UserRepository;
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
public class BookshelfService {

    private final BookshelfRepository bookshelfRepository;
    private final UserRepository userRepository;

    /**
     * Create a new bookshelf for a user
     */
    @SuppressWarnings("null")
    public BookshelfResponse createBookshelf(Long userId, BookshelfRequest request) {
        log.info("Creating bookshelf for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Bookshelf bookshelf = Bookshelf.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .privacy(request.getPrivacy() != null ? request.getPrivacy() : PrivacyLevel.PUBLIC)
                .build();

        Bookshelf saved = bookshelfRepository.save(bookshelf);
        log.info("Bookshelf created with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    /**
     * Get a bookshelf by ID with access control
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public BookshelfResponse getBookshelfById(Long bookshelfId, Long requestingUserId) {
        log.info("Fetching bookshelf: {} for user: {}", bookshelfId, requestingUserId);

        Bookshelf bookshelf = bookshelfRepository.findById(bookshelfId)
                .orElseThrow(() -> new BookshelfNotFoundException("Bookshelf not found with id: " + bookshelfId));

        // Check access permissions
        if (!canAccessBookshelf(bookshelf, requestingUserId)) {
            throw new UnauthorizedAccessException("You don't have permission to access this bookshelf");
        }

        return mapToResponse(bookshelf);
    }

    /**
     * Get all bookshelves for a user
     */
    @Transactional(readOnly = true)
    public List<BookshelfResponse> getUserBookshelves(Long userId) {
        log.info("Fetching all bookshelves for user: {}", userId);

        List<Bookshelf> bookshelves = bookshelfRepository.findByUserId(userId);
        return bookshelves.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated bookshelves for a user
     */
    @Transactional(readOnly = true)
    public Page<BookshelfResponse> getUserBookshelvesPage(Long userId, Pageable pageable) {
        log.info("Fetching paginated bookshelves for user: {}", userId);

        Page<Bookshelf> bookshelves = bookshelfRepository.findByUserId(userId, pageable);
        return bookshelves.map(this::mapToResponse);
    }

    /**
     * Get public bookshelves
     */
    @Transactional(readOnly = true)
    public Page<BookshelfResponse> getPublicBookshelves(Pageable pageable) {
        log.info("Fetching public bookshelves");

        Page<Bookshelf> bookshelves = bookshelfRepository.findPublicBookshelves(pageable);
        return bookshelves.map(this::mapToResponse);
    }

    /**
     * Get accessible bookshelves for a user (public + friends + own)
     */
    @Transactional(readOnly = true)
    public Page<BookshelfResponse> getAccessibleBookshelves(Long userId, Pageable pageable) {
        log.info("Fetching accessible bookshelves for user: {}", userId);

        Page<Bookshelf> bookshelves = bookshelfRepository.findAccessibleBookshelves(userId, pageable);
        return bookshelves.map(this::mapToResponse);
    }

    /**
     * Update a bookshelf
     */
    @SuppressWarnings("null")
    public BookshelfResponse updateBookshelf(Long bookshelfId, Long userId, BookshelfRequest request) {
        log.info("Updating bookshelf: {} by user: {}", bookshelfId, userId);

        Bookshelf bookshelf = bookshelfRepository.findById(bookshelfId)
                .orElseThrow(() -> new BookshelfNotFoundException("Bookshelf not found with id: " + bookshelfId));

        // Only the owner can update
        if (!bookshelf.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only update your own bookshelves");
        }

        if (request.getName() != null) {
            bookshelf.setName(request.getName());
        }
        if (request.getDescription() != null) {
            bookshelf.setDescription(request.getDescription());
        }
        if (request.getPrivacy() != null) {
            bookshelf.setPrivacy(request.getPrivacy());
        }

        Bookshelf updated = bookshelfRepository.save(bookshelf);
        log.info("Bookshelf updated: {}", bookshelfId);

        return mapToResponse(updated);
    }

    /**
     * Delete a bookshelf
     */
    @SuppressWarnings("null")
    public void deleteBookshelf(Long bookshelfId, Long userId) {
        log.info("Deleting bookshelf: {} by user: {}", bookshelfId, userId);

        Bookshelf bookshelf = bookshelfRepository.findById(bookshelfId)
                .orElseThrow(() -> new BookshelfNotFoundException("Bookshelf not found with id: " + bookshelfId));

        // Only the owner can delete
        if (!bookshelf.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only delete your own bookshelves");
        }

        bookshelfRepository.delete(bookshelf);
        log.info("Bookshelf deleted: {}", bookshelfId);
    }

    /**
     * Check if a bookshelf exists and belongs to a user
     */
    @Transactional(readOnly = true)
    public boolean existsByIdAndUserId(Long bookshelfId, Long userId) {
        return bookshelfRepository.existsByIdAndUserId(bookshelfId, userId);
    }

    /**
     * Check if a user can access a bookshelf based on privacy settings
     */
    private boolean canAccessBookshelf(Bookshelf bookshelf, Long requestingUserId) {
        // Owner can always access
        if (bookshelf.getUser().getId().equals(requestingUserId)) {
            return true;
        }

        // Public bookshelves are accessible to everyone
        if (bookshelf.getPrivacy() == PrivacyLevel.PUBLIC) {
            return true;
        }

        // For FRIENDS privacy, check if they're friends (you might need to implement
        // this check)
        if (bookshelf.getPrivacy() == PrivacyLevel.FRIENDS) {
            // TODO: Implement friend check when UserFollow service is available
            return false;
        }

        // PRIVATE bookshelves are only accessible to owner
        return false;
    }

    /**
     * Map entity to response DTO
     */
    private BookshelfResponse mapToResponse(Bookshelf bookshelf) {
        return BookshelfResponse.builder()
                .id(bookshelf.getId())
                .userId(bookshelf.getUser().getId())
                .username(bookshelf.getUser().getUsername())
                .name(bookshelf.getName())
                .description(bookshelf.getDescription())
                .privacy(bookshelf.getPrivacy())
                .bookCount(bookshelf.getBooks().size())
                .createdAt(bookshelf.getCreatedAt())
                .updatedAt(bookshelf.getUpdatedAt())
                .build();
    }
}