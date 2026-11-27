package com.gamelog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawgSearchResponse(
        int count,
        List<RawgGameDto> results
) {}