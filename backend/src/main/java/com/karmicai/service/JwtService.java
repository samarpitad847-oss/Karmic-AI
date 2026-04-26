package com.karmicai.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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

    @Value("${jwt.secret:karmic-ai-default-secret-change-in-production-minimum-32-chars}")
    private String secret;

    private static final long EXPIRY_MS = 7L * 24 * 60 * 60 * 1000; // 7 days

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String role) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
            .signWith(key())
            .compact();
    }

    public Long extractUserId(String bearerToken) {
        Claims claims = parseClaims(bearerToken);
        return Long.parseLong(claims.getSubject());
    }

    public String extractRole(String bearerToken) {
        Claims claims = parseClaims(bearerToken);
        return claims.get("role", String.class);
    }

    public void assertRole(String bearerToken, String expectedRole) {
        String role = extractRole(bearerToken);
        if (!expectedRole.equals(role)) {
            throw new SecurityException("Access denied: role " + expectedRole + " required.");
        }
    }

    private Claims parseClaims(String bearerToken) {
        String token = bearerToken != null && bearerToken.startsWith("Bearer ")
            ? bearerToken.substring(7)
            : bearerToken;
        try {
            return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException e) {
            throw new SecurityException("Invalid or expired token.");
        }
    }
}
