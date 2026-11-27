package com.gamelog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gamelog.models.Game;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByExternalId(Long externalId);
}