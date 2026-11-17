package com.gamelog.models;

import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    private String plataformas;

    private Integer anio;

    @ManyToOne
    @JoinColumn(name = "genre_id")
    @JsonIgnoreProperties("games")
    private Genre genre;

    @ManyToMany
    @JoinTable(
        name = "game_platform",
        joinColumns = @JoinColumn(name = "game_id"),
        inverseJoinColumns = @JoinColumn(name = "platform_id")
    )
    @JsonIgnoreProperties("games")
    private Set<Platform> platforms = new HashSet<>();

    public Game() {
    }

    public Game(String titulo, String plataformas, Integer anio) {
        this.titulo = titulo;
        this.plataformas = plataformas;
        this.anio = anio;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getPlataformas() { return plataformas; }
    public void setPlataformas(String plataformas) { this.plataformas = plataformas; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }

    public Set<Platform> getPlatforms() { return platforms; }
    public void setPlatforms(Set<Platform> platforms) { this.platforms = platforms; }
}
