package com.readersnetwork.bookshelf.repository;

import com.readersnetwork.bookshelf.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByUsernameContainingIgnoreCase(String username);

    // Find users by favorite genre
    @Query("SELECT u FROM User u WHERE u.favoriteGenres LIKE %:genre%")
    List<User> findByFavoriteGenre(@Param("genre") String genre);
}