package com.gamelog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Token de sesión completa (scope "full"): el único que sirve para usar la API. */
    public String generateToken(String email, String rol) {
        return build(email, rol, "full", expirationMs);
    }

    /** Token corto para pasos intermedios del login ("2fa" o "setup"). No sirve como sesión. */
    public String generateLimitedToken(String email, String scope, long ms) {
        return build(email, null, scope, ms);
    }

    private String build(String email, String rol, String scope, long ms) {
        JwtBuilder b = Jwts.builder()
                .subject(email)
                .claim("scope", scope)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ms));
        if (rol != null) {
            b.claim("rol", rol);
        }
        return b.signWith(key()).compact();
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public String extractEmail(String token) {
        return claims(token).getSubject();
    }

    public String extractScope(String token) {
        return claims(token).get("scope", String.class);
    }
}