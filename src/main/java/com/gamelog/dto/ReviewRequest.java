package com.gamelog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull(message = "La calificación es obligatoria")
        @DecimalMin(value = "0.5", message = "La calificación mínima es 0.5")
        @DecimalMax(value = "5.0", message = "La calificación máxima es 5.0")
        Double rating,

        @NotBlank(message = "El texto de la reseña es obligatorio")
        @Size(max = 1000, message = "La reseña no puede superar los 1000 caracteres")
        String text
) {}