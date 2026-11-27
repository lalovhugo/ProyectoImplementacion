package com.gamelog.controller;

import com.gamelog.dto.GameRatingSummary;
import com.gamelog.dto.ReviewRequest;
import com.gamelog.dto.ReviewResponse;
import com.gamelog.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/games/{gameId}/reviews")
    public Page<ReviewResponse> listByGame(@PathVariable Long gameId, Pageable pageable) {
        return reviewService.findByGame(gameId, pageable);
    }

    @GetMapping("/games/{gameId}/reviews/summary")
    public GameRatingSummary summary(@PathVariable Long gameId) {
        return reviewService.getSummary(gameId);
    }

    @PostMapping("/games/{gameId}/reviews")
    public ResponseEntity<ReviewResponse> create(@PathVariable Long gameId,
                                                 @Valid @RequestBody ReviewRequest request,
                                                 Authentication auth) {
        ReviewResponse saved = reviewService.create(gameId, auth.getName(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/reviews/{id}")
                .buildAndExpand(saved.id())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/reviews/{id}")
    public ReviewResponse update(@PathVariable Long id,
                                 @Valid @RequestBody ReviewRequest request,
                                 Authentication auth) {
        return reviewService.update(id, auth.getName(), request);
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        reviewService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}