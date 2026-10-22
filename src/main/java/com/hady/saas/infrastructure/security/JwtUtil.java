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
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30)) // 30 dias — mantém o usuário logado ao fechar/reabrir o navegador
                .signWith(getSigningKey())
                .compact();
    }

    // Token do ADMIN_MASTER — sem estabelecimento vinculado (eid = null) e
    // com expiração mais curta (2h em vez de 8h), já que é uma conta com
    // acesso a todos os dados do sistema; sessões mais curtas reduzem
    // a janela de exposição em caso de token vazado.
    public String gerarTokenAdminMaster(String username, Long usuarioId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", usuarioId)
                .claim("eid", (Long) null)
                .claim("role", "ADMIN_MASTER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 2))
                .signWith(getSigningKey())
                .compact();
    }

    // Token de impersonação — usado pelo ADMIN_MASTER pra entrar como se fosse
    // o dono/funcionário de um estabelecimento (suporte). É um token igual ao
    // normal do usuário-alvo (mesmo subject/uid/eid, então o JwtAuthenticationFilter
    // e o CustomUserDetailsService resolvem role e authorities normalmente, do banco),
    // só que com duas claims extras que o FRONT lê pra mostrar o banner de aviso.
    // Expiração curta (1h) por ser uma sessão pontual de suporte.
    public String gerarTokenImpersonacao(String username, Long usuarioId, Long estabelecimentoId, String impersonadoPor) {
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", usuarioId)
                .claim("eid", estabelecimentoId)
                .claim("impersonating", true)
                .claim("impersonadoPor", impersonadoPor)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60))
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