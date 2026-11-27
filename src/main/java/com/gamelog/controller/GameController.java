package com.gamelog.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gamelog.dto.ImportResultResponse;
import com.gamelog.models.Game;
import com.gamelog.models.Platform;
import com.gamelog.repository.GameRepository;
import com.gamelog.repository.PlatformRepository;
import com.gamelog.service.RawgImportService;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameRepository gameRepository;
    private final PlatformRepository platformRepository;
    private final RawgImportService rawgImportService;

    // Inyección de dependencias a través del constructor
    public GameController(GameRepository gameRepository,
                          PlatformRepository platformRepository,
                          RawgImportService rawgImportService) {
        this.gameRepository = gameRepository;
        this.platformRepository = platformRepository;
        this.rawgImportService = rawgImportService;
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

    // Importación desde RAWG (si usas RawgImportController en /api/admin/games,
    // esta ruta queda en /api/games/admin/games/import; te recomiendo usar RawgImportController)
    @PostMapping("/admin/games/import")
    public ResponseEntity<ImportResultResponse> importFromRawg(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(rawgImportService.importGames(query, limit));
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        if (!gameRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        gameRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}