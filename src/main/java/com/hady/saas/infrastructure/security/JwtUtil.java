package com.hady.saas.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, Long usuarioId, Long estabelecimentoId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", usuarioId)
                .claim("eid", estabelecimentoId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 8))
                .signWith(getSigningKey())
                .compact();
    }

    public String gerarTokenCliente(Long clienteId, String slug) {
        return Jwts.builder()
                .setSubject("cliente:" + clienteId + ":" + slug)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    public String extrairSubjectCliente(String token) {
        return getClaims(token).getSubject();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public Long extractUsuarioId(String token) {
        return getClaims(token).get("uid", Long.class);
    }

    public Long extractEstabelecimentoId(String token) {
        return getClaims(token).get("eid", Long.class);
    }

    public boolean isValid(String token, String username) {
        return extractUsername(token).equals(username) && !isExpired(token);
    }

    public Instant extractIssuedAt(String token) {
        return getClaims(token).getIssuedAt().toInstant();
    }

    private boolean isExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}