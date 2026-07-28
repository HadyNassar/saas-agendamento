package com.hady.saas.application.controller;

import com.hady.saas.business.service.InteligenciaAgendaService;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Item 13 (camada ELITE/PLATINUM) — Inteligência de agenda.
 *
 * Gating: mesmo padrão do resto do robô/marketing, `!= Plano.STANDARD`.
 * ELITE e PLATINUM são idênticos em funcionalidade — a única diferença entre
 * os dois planos é o limite de contas de usuário. Nunca checar
 * `== Plano.ELITE` ou `== Plano.PLATINUM` de forma exclusiva.
 *
 * Este controller só DETECTA e relata — não dispara nada. Se o dono do salão
 * quiser agir sobre um horário vazio detectado, o caminho é criar uma
 * campanha manual pelo CampanhaEnvioController (fluxo de duas etapas já
 * existente: rascunho → confirmação explícita).
 */
@RestController
@RequestMapping("/painel/agenda/inteligencia")
public class InteligenciaAgendaController {

    private final InteligenciaAgendaService inteligenciaAgendaService;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AuthUtil authUtil;

    public InteligenciaAgendaController(InteligenciaAgendaService inteligenciaAgendaService,
                                        EstabelecimentoRepository estabelecimentoRepository,
                                        AuthUtil authUtil) {
        this.inteligenciaAgendaService = inteligenciaAgendaService;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.authUtil = authUtil;
    }

    // GET /painel/agenda/inteligencia/vazio-futuro?dias=7
    @GetMapping("/vazio-futuro")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Map<String, Object>> vazioFuturo(
            @RequestParam(required = false, defaultValue = "7") int dias,
            Authentication auth) {

        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        ResponseEntity<Map<String, Object>> bloqueio = validarPlano(estabelecimentoId);
        if (bloqueio != null) return bloqueio;

        if (dias <= 0 || dias > 60) {
            return ResponseEntity.badRequest().body(Map.of(
                    "erro", "Parâmetro 'dias' deve estar entre 1 e 60."
            ));
        }

        return ResponseEntity.ok(inteligenciaAgendaService.detectarVazioFuturo(estabelecimentoId, dias));
    }

    // GET /painel/agenda/inteligencia/padrao-historico
    @GetMapping("/padrao-historico")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Map<String, Object>> padraoHistorico(Authentication auth) {

        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        ResponseEntity<Map<String, Object>> bloqueio = validarPlano(estabelecimentoId);
        if (bloqueio != null) return bloqueio;

        return ResponseEntity.ok(inteligenciaAgendaService.detectarPadraoHistorico(estabelecimentoId));
    }

    private ResponseEntity<Map<String, Object>> validarPlano(Long estabelecimentoId) {
        Estabelecimento estab = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        if (estab.getPlanoEfetivo() == Plano.STANDARD) {
            return ResponseEntity.status(403).body(Map.of(
                    "erro", "Inteligência de agenda disponível apenas nos planos Elite e Platinum."
            ));
        }
        return null;
    }
}