package com.hady.saas.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Extrai usuarioId e estabelecimentoId do principal já carregado no SecurityContext.
 *
 * Zero queries ao banco — os IDs vêm do CustomUserDetails que foi populado
 * pelo JwtAuthenticationFilter na entrada da requisição.
 */
@Component
public class AuthUtil {

    public Long getEstabelecimentoId(Authentication auth) {
        return getPrincipal(auth).getEstabelecimentoId();
    }

    public Long getUsuarioId(Authentication auth) {
        return getPrincipal(auth).getUsuarioId();
    }

    private CustomUserDetails getPrincipal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new IllegalStateException("Principal não é CustomUserDetails — verifique o filtro JWT.");
        }
        return (CustomUserDetails) auth.getPrincipal();
    }
}
