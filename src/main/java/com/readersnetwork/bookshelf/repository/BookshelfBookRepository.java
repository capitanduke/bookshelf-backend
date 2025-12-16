package com.readersnetwork.bookshelf.repository;

import com.readersnetwork.bookshelf.entity.Bookshelf;
import com.readersnetwork.bookshelf.entity.BookshelfBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookshelfBookRepository extends JpaRepository<BookshelfBook, Long> {

    Optional<BookshelfBook> findByBookshelfIdAndBookId(Long bookshelfId, Long bookId);

    List<BookshelfBook> findByBookshelfIdOrderByPositionAsc(Long bookshelfId);

    Page<BookshelfBook> findByBookshelfId(Long bookshelfId, Pageable pageable);

    boolean existsByBookshelfIdAndBookId(Long bookshelfId, Long bookId);

    void deleteByBookshelfIdAndBookId(Long bookshelfId, Long bookId);

    long countByBookshelfId(Long bookshelfId);

    // Get all bookshelves containing a specific book
    @Query("SELECT bb.bookshelf FROM BookshelfBook bb WHERE bb.book.id = :bookId")
    List<Bookshelf> findBookshelvesContainingBook(@Param("bookId") Long bookId);
}