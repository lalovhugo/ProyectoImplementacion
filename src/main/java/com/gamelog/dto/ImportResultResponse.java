package com.gamelog.dto;

import java.util.List;

public record ImportResultResponse(
        String query,
        int importedCount,
        int skippedCount,
        List<String> importedTitles
) {}