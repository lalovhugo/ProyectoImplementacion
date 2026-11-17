package com.gamelog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gameLogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GameLog API")
                        .version("1.0")
                        .description("API REST para registrar y reseñar videojuegos"))
                .servers(List.of(new Server().url("/").description("Servidor actual")));
    }
}