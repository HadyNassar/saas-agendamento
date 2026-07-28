package com.hady.saas.application.controller;

import com.hady.saas.domain.alerta.AlertaPainel;
import com.hady.saas.infrastructure.repository.AlertaPainelRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Item 16 (gatilho de reputação) — Alertas do painel.
 *
 * Tabela genérica (AlertaPainel/TipoAlerta) pensada para ser reaproveitada por
 * outros tipos de alerta no futuro; hoje o único produtor é o gatilho de
 * avaliação com nota baixa (1-2), disparado em AvaliacaoController.avaliar().
 *
 * Sem gating de plano aqui: avaliações só existem em ELITE/PLATINUM (ver
 * AvaliacaoController), então na prática só donos desses planos terão
 * alertas — não há necessidade de duplicar a checagem.
 *
 *   GET   /painel/alertas               → lista não vistos (para badge/lista ao abrir painel)
 *   GET   /painel/alertas/historico     → lista completa (vistos e não vistos)
 *   PATCH /painel/alertas/{id}/visto    → marca como visto
 */
@RestController
@RequestMapping("/painel/alertas")
public class AlertaPainelController {

    private final AlertaPainelRepository alertaPainelRepository;
    private final AuthUtil authUtil;

    public AlertaPainelController(AlertaPainelRepository alertaPainelRepository,
                                  AuthUtil authUtil) {
        this.alertaPainelRepository = alertaPainelRepository;
        this.authUtil = authUtil;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listarNaoVistos(Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);

        List<AlertaPainel> alertas = alertaPainelRepository
                .findByEstabelecimentoIdAndVistoFalseOrderByCriadoEmDesc(estabelecimentoId);

        return ResponseEntity.ok(Map.of(
                "total",   alertas.size(),
                "alertas", alertas.stream().map(this::toMap).toList()
        ));
    }

    @GetMapping("/historico")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listarHistorico(Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);

        List<AlertaPainel> alertas = alertaPainelRepository
                .findByEstabelecimentoIdOrderByCriadoEmDesc(estabelecimentoId);

        return ResponseEntity.ok(Map.of(
                "total",   alertas.size(),
                "alertas", alertas.stream().map(this::toMap).toList()
        ));
    }

    @PatchMapping("/{id}/visto")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> marcarVisto(@PathVariable Long id, Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);

        AlertaPainel alerta = alertaPainelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));

        if (!alerta.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Alerta não pertence a este estabelecimento");
        }

        alerta.setVisto(true);
        alertaPainelRepository.save(alerta);

        return ResponseEntity.ok("Alerta marcado como visto.");
    }

    private Map<String, Object> toMap(AlertaPainel a) {
        return Map.of(
                "id",          a.getId(),
                "tipo",        a.getTipo().name(),
                "mensagem",    a.getMensagem(),
                "referenciaId", a.getReferenciaId() != null ? a.getReferenciaId() : "",
                "visto",       a.isVisto(),
                "criadoEm",    a.getCriadoEm().toString()
        );
    }
}