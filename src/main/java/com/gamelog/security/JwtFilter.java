package com.gamelog.security;

import com.gamelog.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                // Los tokens temporales del login (2fa / setup) nunca autentican
                if ("full".equals(jwtService.extractScope(token))) {
                    String email = jwtService.extractEmail(token);
                    userRepository.findByEmail(email)
                            // Un ADMIN sin 2FA activado nunca queda autenticado
                            .filter(u -> !"ADMIN".equals(u.getRol()) || u.isTotpEnabled())
                            .ifPresent(user -> {
                                var auth = new UsernamePasswordAuthenticationToken(
                                        user.getEmail(), null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRol())));
                                SecurityContextHolder.getContext().setAuthentication(auth);
                            });
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Token inválido o expirado: la petición sigue sin autenticar
            }
        }
        chain.doFilter(request, response);
    }
}