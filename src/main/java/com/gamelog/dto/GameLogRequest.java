package com.gamelog.dto;

import com.gamelog.models.LogStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GameLogRequest(
        @NotNull(message = "El id del juego es obligatorio")
        Long gameId,

        @NotNull(message = "El estado es obligatorio")
        LogStatus status,

        @DecimalMin(value = "0.5", message = "La calificación mínima es 0.5")
        @DecimalMax(value = "5.0", message = "La calificación máxima es 5.0")
        Double rating,

        LocalDate playedDate
) {}