package com.gamelog.controller;

import com.gamelog.models.User;
import com.gamelog.repository.UserRepository;
import com.gamelog.security.AttemptLimiter;
import com.gamelog.security.JwtService;
import com.gamelog.security.TotpService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RegisterRequest(String username, String email, String password) {}
    public record LoginRequest(String email, String password) {}
    public record VerifyRequest(String tempToken, String code) {}
    public record SetupRequest(String setupToken) {}
    public record EnableRequest(String setupToken, String code) {}
    public record DisableRequest(String password, String code) {}

    private static final long CODE_STEP_MS = 5 * 60 * 1000;
    private static final long SETUP_STEP_MS = 10 * 60 * 1000;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final AttemptLimiter limiter;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          TotpService totpService,
                          AttemptLimiter limiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.totpService = totpService;
        this.limiter = limiter;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (isBlank(request.username()) || isBlank(request.email()) || isBlank(request.password())) {
            return ResponseEntity.badRequest().body(Map.of("error", "username, email y password son obligatorios"));
        }
        if (request.password().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña debe tener al menos 6 caracteres"));
        }
        if (userRepository.existsByEmail(request.email()) || userRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "El usuario o el correo ya existen"));
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Usuario registrado", "email", user.getEmail(), "rol", user.getRol()));
    }

    /**
     * step = OK    -> sesión completa (token)
     * step = CODE  -> falta el código de 6 dígitos (tempToken)
     * step = SETUP -> es ADMIN y debe activar el 2FA antes de entrar (setupToken)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(u -> request.password() != null
                        && passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElse(null);
        if (user == null) {
            return error(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }
        if (user.isTotpEnabled()) {
            return ResponseEntity.ok(Map.of("step", "CODE",
                    "tempToken", jwtService.generateLimitedToken(user.getEmail(), "2fa", CODE_STEP_MS)));
        }
        if ("ADMIN".equals(user.getRol())) {
            return ResponseEntity.ok(Map.of("step", "SETUP",
                    "setupToken", jwtService.generateLimitedToken(user.getEmail(), "setup", SETUP_STEP_MS)));
        }
        return ResponseEntity.ok(fullSession(user));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest req) {
        User user = userFromLimited(req.tempToken(), "2fa");
        if (user == null) {
            return error(HttpStatus.UNAUTHORIZED, "La verificación expiró. Inicia sesión de nuevo.");
        }
        String key = user.getEmail();
        if (limiter.isLocked(key)) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espera 5 minutos.");
        }
        String code = req.code() == null ? "" : req.code().trim();
        boolean ok = totpService.isValid(user.getTotpSecret(), code)
                || (code.length() > 6 && useRecoveryCode(user, code));
        if (!ok) {
            limiter.fail(key);
            return error(HttpStatus.UNAUTHORIZED, "Código incorrecto");
        }
        limiter.reset(key);
        return ResponseEntity.ok(fullSession(user));
    }

    /** Genera el secreto y el QR. Sirve para el admin (setupToken) o para un usuario con sesión. */
    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setup(@RequestBody(required = false) SetupRequest req, Authentication auth) {
        User user = resolve(req == null ? null : req.setupToken(), auth);
        if (user == null) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión primero");
        }
        if (user.isTotpEnabled()) {
            return error(HttpStatus.CONFLICT, "El 2FA ya está activado");
        }
        user.setTotpSecret(totpService.newSecret());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "secret", user.getTotpSecret(),
                "qr", totpService.qrDataUri(user.getEmail(), user.getTotpSecret())));
    }

    /** Confirma con el primer código y activa el 2FA. Devuelve los códigos de recuperación (una sola vez). */
    @PostMapping("/2fa/enable")
    public ResponseEntity<?> enable(@RequestBody EnableRequest req, Authentication auth) {
        boolean forced = !isBlank(req.setupToken());
        User user = resolve(req.setupToken(), auth);
        if (user == null) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión primero");
        }
        if (user.isTotpEnabled()) {
            return error(HttpStatus.CONFLICT, "El 2FA ya está activado");
        }
        if (user.getTotpSecret() == null) {
            return error(HttpStatus.BAD_REQUEST, "Primero genera el código QR");
        }
        String key = user.getEmail();
        if (limiter.isLocked(key)) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espera 5 minutos.");
        }
        if (!totpService.isValid(user.getTotpSecret(), req.code())) {
            limiter.fail(key);
            return error(HttpStatus.UNAUTHORIZED, "Código incorrecto");
        }
        limiter.reset(key);

        String[] codes = totpService.newRecoveryCodes();
        user.setRecoveryCodes(Arrays.stream(codes)
                .map(passwordEncoder::encode)
                .collect(Collectors.joining(",")));
        user.setTotpEnabled(true);
        userRepository.save(user);

        Map<String, Object> body = new HashMap<>();
        body.put("recoveryCodes", codes);
        if (forced) {
            body.putAll(fullSession(user));
        }
        return ResponseEntity.ok(body);
    }

    /** Solo para usuarios normales con sesión; los ADMIN no pueden desactivarlo. */
    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disable(@RequestBody DisableRequest req, Authentication auth) {
        User user = resolve(null, auth);
        if (user == null) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión primero");
        }
        if ("ADMIN".equals(user.getRol())) {
            return error(HttpStatus.BAD_REQUEST, "Los administradores no pueden desactivar el 2FA");
        }
        if (!user.isTotpEnabled()) {
            return error(HttpStatus.BAD_REQUEST, "El 2FA no está activado");
        }
        String key = user.getEmail();
        if (limiter.isLocked(key)) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espera 5 minutos.");
        }
        boolean ok = req.password() != null
                && passwordEncoder.matches(req.password(), user.getPasswordHash())
                && totpService.isValid(user.getTotpSecret(), req.code());
        if (!ok) {
            limiter.fail(key);
            return error(HttpStatus.UNAUTHORIZED, "Contraseña o código incorrectos");
        }
        limiter.reset(key);
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        user.setRecoveryCodes(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("mensaje", "2FA desactivado"));
    }

    // ---------- auxiliares ----------

    private Map<String, Object> fullSession(User u) {
        return Map.of(
                "step", "OK",
                "token", jwtService.generateToken(u.getEmail(), u.getRol()),
                "email", u.getEmail(),
                "rol", u.getRol(),
                "twoFactor", u.isTotpEnabled());
    }

    private User userFromLimited(String token, String scope) {
        if (isBlank(token)) {
            return null;
        }
        try {
            if (!scope.equals(jwtService.extractScope(token))) {
                return null;
            }
            return userRepository.findByEmail(jwtService.extractEmail(token)).orElse(null);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    private User resolve(String setupToken, Authentication auth) {
        if (!isBlank(setupToken)) {
            return userFromLimited(setupToken, "setup");
        }
        if (auth != null && auth.isAuthenticated()) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    private boolean useRecoveryCode(User user, String raw) {
        if (isBlank(user.getRecoveryCodes())) {
            return false;
        }
        String code = raw.trim().toUpperCase();
        List<String> hashes = new ArrayList<>(Arrays.asList(user.getRecoveryCodes().split(",")));
        for (String hash : hashes) {
            if (passwordEncoder.matches(code, hash)) {
                hashes.remove(hash); // cada código sirve una sola vez
                user.setRecoveryCodes(String.join(",", hashes));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(Map.of("error", msg));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}