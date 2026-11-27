package com.gamelog.service;

import com.gamelog.dto.GameLogRequest;
import com.gamelog.dto.GameLogResponse;
import com.gamelog.exception.ResourceNotFoundException;
import com.gamelog.models.Game;
import com.gamelog.models.GameLogEntry;
import com.gamelog.models.LogStatus;
import com.gamelog.models.User;
import com.gamelog.repository.GameLogRepository;
import com.gamelog.repository.GameRepository;
import com.gamelog.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class GameLogService {

    private final GameLogRepository logRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public GameLogService(GameLogRepository logRepository,
                          GameRepository gameRepository,
                          UserRepository userRepository) {
        this.logRepository = logRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<GameLogResponse> findMine(String email, LogStatus status) {
        User user = findUser(email);
        List<GameLogEntry> entries = (status == null)
                ? logRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                : logRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), status);
        return entries.stream().map(GameLogResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public GameLogResponse findMineByGame(String email, Long gameId) {
        User user = findUser(email);
        return logRepository.findByUserIdAndGameId(user.getId(), gameId)
                .map(GameLogResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Este juego no está en tu diario"));
    }

    /** Crea o actualiza la entrada del usuario para ese juego. */
    public GameLogResponse upsert(String email, GameLogRequest req) {
        validateHalfStep(req.rating());
        User user = findUser(email);
        Game game = gameRepository.findById(req.gameId())
                .orElseThrow(() -> new ResourceNotFoundException("Juego no encontrado: " + req.gameId()));

        GameLogEntry entry = logRepository.findByUserIdAndGameId(user.getId(), game.getId())
                .orElseGet(() -> {
                    GameLogEntry e = new GameLogEntry();
                    e.setUser(user);
                    e.setGame(game);
                    return e;
                });

        entry.setStatus(req.status());
        entry.setRating(req.rating());
        entry.setPlayedDate(req.playedDate() != null
                ? req.playedDate()
                : (req.status() == LogStatus.PENDIENTE ? null : LocalDate.now()));

        return GameLogResponse.from(logRepository.saveAndFlush(entry));
    }

    public GameLogResponse addToBacklog(String email, Long gameId) {
        return upsert(email, new GameLogRequest(gameId, LogStatus.PENDIENTE, null, null));
    }

    public GameLogResponse update(Long id, String email, GameLogRequest req) {
        validateHalfStep(req.rating());
        GameLogEntry entry = logRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada de diario no encontrada: " + id));
        if (!entry.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes editar tu propio diario");
        }
        entry.setStatus(req.status());
        entry.setRating(req.rating());
        if (req.playedDate() != null) {
            entry.setPlayedDate(req.playedDate());
        }
        return GameLogResponse.from(logRepository.saveAndFlush(entry));
    }

    public void delete(Long id, String email) {
        GameLogEntry entry = logRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada de diario no encontrada: " + id));
        if (!entry.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes eliminar entradas de tu propio diario");
        }
        logRepository.delete(entry);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }

    private void validateHalfStep(Double rating) {
        if (rating != null && Math.abs(rating * 2 - Math.round(rating * 2)) > 1e-9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La calificación debe ser en incrementos de 0.5 (ej. 3.5, 4.0, 4.5)");
        }
    }
}