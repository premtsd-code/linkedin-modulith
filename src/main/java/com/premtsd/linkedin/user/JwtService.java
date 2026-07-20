package com.premtsd.linkedin.user;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

/**
 * Exposed API of the user module: mint and validate JWTs. Referenced by the
 * application shell's {@link com.premtsd.linkedin.JwtAuthFilter}.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private static final long TTL_MILLIS = 1000L * 60 * 60; // 1 hour

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    String generate(Long userId, String email, Set<String> roles) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", roles.stream().toList())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TTL_MILLIS))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
