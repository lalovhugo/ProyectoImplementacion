package com.gamelog.controller;

import com.gamelog.models.Game;
import com.gamelog.models.Platform;
import com.gamelog.repository.GameRepository;
import com.gamelog.repository.PlatformRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameRepository gameRepository;
    private final PlatformRepository platformRepository;

    // Inyección de dependencias a través del constructor
    public GameController(GameRepository gameRepository, PlatformRepository platformRepository) {
        this.gameRepository = gameRepository;
        this.platformRepository = platformRepository;
    }

    // Método GET solicitado
    @GetMapping
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    // Método POST solicitado
    @PostMapping
    public Game createGame(@RequestBody Game game) {
        return gameRepository.save(game);
    }

    // Muchos a muchos: ver las plataformas de un juego
    @GetMapping("/{gameId}/platforms")
    public ResponseEntity<Set<Platform>> getPlatformsOfGame(@PathVariable Long gameId) {
        return gameRepository.findById(gameId)
                .map(game -> ResponseEntity.ok(game.getPlatforms()))
                .orElse(ResponseEntity.notFound().build());
    }

    // Muchos a muchos: asociar una plataforma a un juego
    @PostMapping("/{gameId}/platforms/{platformId}")
    public ResponseEntity<Game> addPlatformToGame(@PathVariable Long gameId,
                                                  @PathVariable Long platformId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        Platform platform = platformRepository.findById(platformId).orElse(null);
        if (game == null || platform == null) {
            return ResponseEntity.notFound().build();
        }
        game.getPlatforms().add(platform);
        return ResponseEntity.ok(gameRepository.save(game));
    }

    // Muchos a muchos: quitar una plataforma de un juego
    @DeleteMapping("/{gameId}/platforms/{platformId}")
    public ResponseEntity<Game> removePlatformFromGame(@PathVariable Long gameId,
                                                       @PathVariable Long platformId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        game.getPlatforms().removeIf(p -> p.getId().equals(platformId));
        return ResponseEntity.ok(gameRepository.save(game));
    }
}