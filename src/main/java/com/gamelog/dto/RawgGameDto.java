package com.gamelog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawgGameDto(
        Long id,
        String name,
        String released,
        @JsonProperty("background_image") String backgroundImage,
        List<RawgPlatformWrapper> platforms,
        List<RawgGenreDto> genres
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawgPlatformWrapper(RawgPlatformDto platform) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawgPlatformDto(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawgGenreDto(String name) {}
}