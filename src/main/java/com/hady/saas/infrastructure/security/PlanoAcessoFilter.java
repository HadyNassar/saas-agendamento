package com.hady.saas.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Bloqueia acesso às rotas /painel/** quando o estabelecimento não tem acesso ativo.
 *
 * Regras (definidas em Estabelecimento.temAcesso()):
 *   - Plano pago ativo com planoValidoAte no futuro → permite
 *   - Trial ativo com trialFim no futuro            → permite
 *   - Qualquer outro caso                           → 402 Payment Required
 *
 * Rotas excluídas da verificação (o usuário precisa acessar mesmo sem plano):
 *   - /painel/trial/**          → iniciar/consultar trial
 *   - /painel/assinatura/**     → assinar/pagar plano
 *   - /painel/mercadopago/**    → conectar conta MP / callback OAuth
 */
@Component
public class PlanoAcessoFilter extends OncePerRequestFilter {

    private static final String[] ROTAS_LIBERADAS = {
            "/painel/trial/",
            "/painel/assinatura/",
            "/painel/mercadopago/"
    };

    // FIX: injetamos EstabelecimentoRepository em vez de UsuarioRepository.
    // A query original buscava o usuário por email com JOIN FETCH do estabelecimento
    // a cada request autenticado em /painel/**. Com o CustomUserDetails já carregado
    // no SecurityContext pelo JwtAuthenticationFilter (que contém estabelecimentoId),
    // podemos buscar diretamente o estabelecimento por ID — uma query mais simples
    // e sem JOIN, reduzindo custo por request.
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AuthUtil authUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlanoAcessoFilter(EstabelecimentoRepository estabelecimentoRepository,
                             AuthUtil authUtil) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.authUtil = authUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Só aplica em rotas /painel/**
        if (!path.startsWith("/painel/")) {
            return true;
        }

        // Libera rotas de pagamento/trial mesmo sem plano ativo
        for (String rota : ROTAS_LIBERADAS) {
            if (path.startsWith(rota)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Se não há usuário autenticado, deixa o Spring Security rejeitar normalmente
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // FIX: lê estabelecimentoId direto do principal (sem query ao banco por email).
        // O CustomUserDetails foi populado pelo JwtAuthenticationFilter e já contém
        // o estabelecimentoId extraído do claim "eid" do JWT — zero queries extras aqui.
        Long estabelecimentoId;
        try {
            estabelecimentoId = authUtil.getEstabelecimentoId(authentication);
        } catch (Exception e) {
            // Principal inválido ou sem claim eid — deixa o Spring Security tratar
            filterChain.doFilter(request, response);
            return;
        }

        Estabelecimento estab = estabelecimentoRepository.findById(estabelecimentoId).orElse(null);
        if (estab == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!estab.temAcesso()) {
            response.setStatus(HttpStatus.PAYMENT_REQUIRED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "error", "Acesso bloqueado",
                    "message", "Seu período de trial expirou ou seu plano está inativo. Acesse /painel/assinatura para assinar um plano.",
                    "codigo", "PLANO_INATIVO"
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
