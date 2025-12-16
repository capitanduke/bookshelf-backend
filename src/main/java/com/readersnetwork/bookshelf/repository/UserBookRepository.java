package com.readersnetwork.bookshelf.repository;

import com.readersnetwork.bookshelf.entity.UserBook;
import com.readersnetwork.bookshelf.entity.ReadingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBookRepository extends JpaRepository<UserBook, Long> {

    Optional<UserBook> findByUserIdAndBookId(Long userId, Long bookId);

    List<UserBook> findByUserId(Long userId);

    List<UserBook> findByUserIdAndStatus(Long userId, ReadingStatus status);

    // Get all books with specific status for a user
    @Query("SELECT ub FROM UserBook ub WHERE ub.user.id = :userId AND ub.status = :status")
    Page<UserBook> findByUserAndStatus(@Param("userId") Long userId,
            @Param("status") ReadingStatus status,
            Pageable pageable);

    // Get recently finished books
    @Query("SELECT ub FROM UserBook ub WHERE ub.user.id = :userId " +
            "AND ub.status = 'COMPLETED' ORDER BY ub.finishDate DESC")
    List<UserBook> findRecentlyFinishedBooks(@Param("userId") Long userId, Pageable pageable);

    // Get currently reading books
    @Query("SELECT ub FROM UserBook ub WHERE ub.user.id = :userId " +
            "AND ub.status = 'READING' ORDER BY ub.startDate DESC")
    List<UserBook> findCurrentlyReading(@Param("userId") Long userId);

    // Count books by status
    long countByUserIdAndStatus(Long userId, ReadingStatus status);

    // Get reading statistics
    @Query("SELECT COUNT(ub) FROM UserBook ub WHERE ub.user.id = :userId " +
            "AND ub.status = 'COMPLETED' AND YEAR(ub.finishDate) = :year")
    long countBooksCompletedInYear(@Param("userId") Long userId, @Param("year") int year);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);
}