package com.hady.saas.application.controller;

import com.hady.saas.business.service.MetricasWhatsAppService;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Item 12 (camada ELITE/PLATINUM) — Métricas de WhatsApp no painel.
 *
 * Gating: mesmo padrão do resto do robô/marketing, `!= Plano.STANDARD`.
 * ELITE e PLATINUM são idênticos em funcionalidade — a única diferença entre
 * os dois planos é o limite de contas de usuário, não o acesso a métricas.
 * Nunca checar `== Plano.ELITE` ou `== Plano.PLATINUM` de forma exclusiva.
 */
@RestController
@RequestMapping("/painel/whatsapp/metricas")
public class MetricasWhatsAppController {

    private final MetricasWhatsAppService metricasWhatsAppService;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AuthUtil authUtil;

    public MetricasWhatsAppController(MetricasWhatsAppService metricasWhatsAppService,
                                      EstabelecimentoRepository estabelecimentoRepository,
                                      AuthUtil authUtil) {
        this.metricasWhatsAppService = metricasWhatsAppService;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.authUtil = authUtil;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Map<String, Object>> metricas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication auth) {

        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        Estabelecimento estab = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        if (estab.getPlanoEfetivo() == Plano.STANDARD) {
            return ResponseEntity.status(403).body(Map.of(
                    "erro", "Métricas de WhatsApp disponíveis apenas nos planos Elite e Platinum."
            ));
        }

        return ResponseEntity.ok(metricasWhatsAppService.gerarMetricas(estabelecimentoId, inicio, fim));
    }

    @GetMapping("/mes")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Map<String, Object>> metricasMes(Authentication auth) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());
        return metricas(inicioMes, fimMes, auth);
    }
}