package com.gamelog.repository;

import com.gamelog.models.GameLogEntry;
import com.gamelog.models.LogStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameLogRepository extends JpaRepository<GameLogEntry, Long> {

    List<GameLogEntry> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<GameLogEntry> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, LogStatus status);

    Optional<GameLogEntry> findByUserIdAndGameId(Long userId, Long gameId);
}