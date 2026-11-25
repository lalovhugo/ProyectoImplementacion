package com.gamelog.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String rol = "USER";

    // Verificación en dos pasos (TOTP)
    @Column(length = 64)
    private String totpSecret;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean totpEnabled = false;

    // Hashes BCrypt de los códigos de recuperación, separados por comas
    @Column(length = 1000)
    private String recoveryCodes;
}