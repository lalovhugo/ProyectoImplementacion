package com.gamelog.repository;

import com.gamelog.models.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByGameIdOrderByCreatedAtDesc(Long gameId, Pageable pageable);

    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    long countByGameId(Long gameId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.game.id = :gameId")
    Double findAverageRatingByGameId(Long gameId);
}