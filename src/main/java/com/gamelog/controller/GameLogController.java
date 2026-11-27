package com.gamelog.controller;

import com.gamelog.dto.GameLogRequest;
import com.gamelog.dto.GameLogResponse;
import com.gamelog.models.LogStatus;
import com.gamelog.service.GameLogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GameLogController {

    private final GameLogService service;

    public GameLogController(GameLogService service) {
        this.service = service;
    }

    @GetMapping("/log")
    public List<GameLogResponse> myLog(@RequestParam(required = false) LogStatus status,
                                       Authentication auth) {
        return service.findMine(auth.getName(), status);
    }

    @GetMapping("/log/game/{gameId}")
    public GameLogResponse byGame(@PathVariable Long gameId, Authentication auth) {
        return service.findMineByGame(auth.getName(), gameId);
    }

    @PostMapping("/log")
    public GameLogResponse save(@Valid @RequestBody GameLogRequest request, Authentication auth) {
        return service.upsert(auth.getName(), request);
    }

    @PutMapping("/log/{id}")
    public GameLogResponse update(@PathVariable Long id,
                                  @Valid @RequestBody GameLogRequest request,
                                  Authentication auth) {
        return service.update(id, auth.getName(), request);
    }

    @DeleteMapping("/log/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        service.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/backlog/{gameId}")
    public GameLogResponse addToBacklog(@PathVariable Long gameId, Authentication auth) {
        return service.addToBacklog(auth.getName(), gameId);
    }
}