package com.gamelog.dto;

import com.gamelog.models.GameLogEntry;
import com.gamelog.models.LogStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GameLogResponse(
        Long id,
        Long gameId,
        String gameTitle,
        Integer gameYear,
        String gameGenre,
        LogStatus status,
        Double rating,
        LocalDate playedDate,
        LocalDateTime updatedAt
) {
    public static GameLogResponse from(GameLogEntry e) {
        return new GameLogResponse(
                e.getId(),
                e.getGame().getId(),
                e.getGame().getTitulo(),
                e.getGame().getAnio(),
                e.getGame().getGenre() != null ? e.getGame().getGenre().getName() : null,
                e.getStatus(),
                e.getRating(),
                e.getPlayedDate(),
                e.getUpdatedAt()
        );
    }
}