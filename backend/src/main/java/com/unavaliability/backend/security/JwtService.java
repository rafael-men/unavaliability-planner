package com.unavaliability.backend.security;

import com.unavaliability.backend.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;


@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    private static final int MIN_SECRET_BYTES = 32;

    public JwtService(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.expiration-ms:604800000}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret ausente. Defina a variável de ambiente SESSION_SECRET "
                            + "(>= " + MIN_SECRET_BYTES + " bytes).");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret fraco: precisa de pelo menos " + MIN_SECRET_BYTES
                            + " bytes (256 bits) para HS256. Atual: " + bytes.length + " bytes.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claims(Map.of(
                        "email", user.getEmail() == null ? "" : user.getEmail(),
                        "role", user.getRole() == null ? "" : user.getRole()))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
