package com.gamelog.client;

import com.gamelog.dto.RawgSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class RawgClient {

    private final RestClient restClient;
    private final String apiKey;

    public RawgClient(@Value("${rawg.api.base-url:https://api.rawg.io/api}") String baseUrl,
                      @Value("${rawg.api.key:demo}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public RawgSearchResponse searchGames(String query, int pageSize) {
        try {
            RawgSearchResponse resp = restClient.get()
                    .uri(uri -> uri.path("/games")
                            .queryParam("key", apiKey)
                            .queryParam("search", query)
                            .queryParam("page_size", pageSize)
                            .build())
                    .retrieve()
                    .body(RawgSearchResponse.class);
            return resp != null ? resp : new RawgSearchResponse(0, List.of());
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Error al consultar RAWG (" + ex.getStatusCode().value() + ")");
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo conectar con la API de RAWG");
        }
    }
}