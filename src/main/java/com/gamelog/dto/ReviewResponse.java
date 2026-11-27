package com.gamelog.dto;

import com.gamelog.models.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long gameId,
        String gameTitle,
        String username,
        String userEmail,
        Double rating,
        String text,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getGame().getId(),
                r.getGame().getTitulo(),
                r.getUser().getUsername(),
                r.getUser().getEmail(),
                r.getRating(),
                r.getText(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}