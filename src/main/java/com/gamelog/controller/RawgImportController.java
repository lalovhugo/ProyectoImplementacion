package com.gamelog.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gamelog.dto.ImportResultResponse;
import com.gamelog.service.RawgImportService;

@RestController
@RequestMapping("/api/admin/games")
public class RawgImportController {

    private final RawgImportService importService;

    public RawgImportController(RawgImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importGames(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(importService.importGames(query, limit));
    }
}