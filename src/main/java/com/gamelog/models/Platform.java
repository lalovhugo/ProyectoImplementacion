package com.gamelog.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "platforms")
@Getter
@Setter
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Size(max = 80)
    private String manufacturer;

    @ManyToMany(mappedBy = "platforms")
    @JsonIgnoreProperties("platforms")
    private Set<Game> games = new HashSet<>();
}
