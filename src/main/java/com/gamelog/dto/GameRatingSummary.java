package com.gamelog.dto;

public record GameRatingSummary(
        Long gameId,
        double averageRating,
        long totalReviews
) {}