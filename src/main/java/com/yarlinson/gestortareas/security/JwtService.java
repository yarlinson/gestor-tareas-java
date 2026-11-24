package com.yarlinson.gestortareas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Emite y valida tokens JWT firmados con HMAC-SHA. */
@Service
public class JwtService {

    private static final int LONGITUD_MINIMA_SECRETO = 32;

    private final SecretKey clave;
    private final Duration vigencia;

    public JwtService(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.expiration-minutes}") long minutosDeVigencia) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos " + LONGITUD_MINIMA_SECRETO + " caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.vigencia = Duration.ofMinutes(minutosDeVigencia);
    }

    public String generar(String nombreUsuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(nombreUsuario)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(clave)
                .compact();
    }

    /**
     * Devuelve el nombre de usuario del token, o vacío si el token es inválido,
     * está manipulado o ya expiró.
     */
    public Optional<String> extraerNombreUsuario(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long segundosDeVigencia() {
        return vigencia.toSeconds();
    }
}
