package com.gamelog.controller;

import com.gamelog.models.Game;
import com.gamelog.repository.GameRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameRepository gameRepository;

    // Inyección de dependencias a través del constructor
    public GameController(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
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
}