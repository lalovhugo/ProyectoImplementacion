package com.gamelog.service;

import com.gamelog.client.RawgClient;
import com.gamelog.dto.ImportResultResponse;
import com.gamelog.dto.RawgGameDto;
import com.gamelog.dto.RawgSearchResponse;
import com.gamelog.models.Game;
import com.gamelog.models.Genre;
import com.gamelog.repository.GameRepository;
import com.gamelog.repository.GenreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RawgImportService {

    private final RawgClient rawgClient;
    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;

    public RawgImportService(RawgClient rawgClient,
                             GameRepository gameRepository,
                             GenreRepository genreRepository) {
        this.rawgClient = rawgClient;
        this.gameRepository = gameRepository;
        this.genreRepository = genreRepository;
    }

    public ImportResultResponse importGames(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El parámetro 'query' es obligatorio");
        }
        int safeLimit = Math.max(1, Math.min(limit, 20));
        RawgSearchResponse response = rawgClient.searchGames(query.trim(), safeLimit);
        List<RawgGameDto> candidates = response.results() != null ? response.results() : List.of();

        int imported = 0;
        int skipped = 0;
        List<String> titles = new ArrayList<>();

        for (RawgGameDto item : candidates) {
            if (item.id() == null || item.name() == null || item.name().isBlank()) {
                skipped++;
                continue;
            }
            if (gameRepository.findByExternalId(item.id()).isPresent()) {
                skipped++;
                continue;
            }

            Game game = new Game();
            game.setExternalId(item.id());
            game.setTitulo(item.name().trim());
            game.setAnio(parseYear(item.released()));
            game.setCoverUrl(item.backgroundImage());

            if (item.platforms() != null && !item.platforms().isEmpty()) {
                String plats = item.platforms().stream()
                        .map(w -> w.platform() != null ? w.platform().name() : null)
                        .filter(n -> n != null && !n.isBlank())
                        .limit(4)
                        .collect(Collectors.joining(", "));
                game.setPlataformas(plats);
            }

            if (item.genres() != null && !item.genres().isEmpty()) {
                String firstGenre = item.genres().get(0).name();
                if (firstGenre != null && !firstGenre.isBlank()) {
                    Genre genre = genreRepository.findAll().stream()
                            .filter(g -> g.getName() != null && g.getName().equalsIgnoreCase(firstGenre.trim()))
                            .findFirst()
                            .orElseGet(() -> {
                                Genre ng = new Genre();
                                ng.setName(firstGenre.trim());
                                return genreRepository.save(ng);
                            });
                    game.setGenre(genre);
                }
            }

            gameRepository.save(game);
            imported++;
            titles.add(game.getTitulo());
        }

        return new ImportResultResponse(query.trim(), imported, skipped, titles);
    }

    private Integer parseYear(String released) {
        if (released == null || released.length() < 4) return null;
        try {
            return Integer.parseInt(released.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}