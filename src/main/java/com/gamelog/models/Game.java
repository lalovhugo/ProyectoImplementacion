package com.gamelog.models;

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

    // Constructores vacíos y con parámetros requeridos por JPA
    public Game() {
    }

    public Game(String titulo, String plataformas, Integer anio) {
        this.titulo = titulo;
        this.plataformas = plataformas;
        this.anio = anio;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getPlataformas() { return plataformas; }
    public void setPlataformas(String plataformas) { this.plataformas = plataformas; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
}