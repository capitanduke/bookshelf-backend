package com.readersnetwork.bookshelf.repository;

import com.readersnetwork.bookshelf.entity.Bookshelf;
import com.readersnetwork.bookshelf.entity.PrivacyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookshelfRepository extends JpaRepository<Bookshelf, Long> {

    List<Bookshelf> findByUserId(Long userId);

    Page<Bookshelf> findByUserId(Long userId, Pageable pageable);

    List<Bookshelf> findByUserIdAndPrivacy(Long userId, PrivacyLevel privacy);

    // Find public bookshelves
    @Query("SELECT b FROM Bookshelf b WHERE b.privacy = 'PUBLIC' ORDER BY b.createdAt DESC")
    Page<Bookshelf> findPublicBookshelves(Pageable pageable);

    // Find bookshelves accessible to a user (public + friends' friends-only + own)
    @Query("SELECT b FROM Bookshelf b WHERE b.privacy = 'PUBLIC' " +
            "OR b.user.id = :userId " +
            "OR (b.privacy = 'FRIENDS' AND b.user.id IN " +
            "(SELECT uf.following.id FROM UserFollow uf WHERE uf.follower.id = :userId))")
    Page<Bookshelf> findAccessibleBookshelves(@Param("userId") Long userId, Pageable pageable);

    boolean existsByIdAndUserId(Long id, Long userId);
}