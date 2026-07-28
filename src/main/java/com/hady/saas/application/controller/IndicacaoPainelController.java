package com.hady.saas.application.controller;

import com.hady.saas.business.service.IndicacaoService;
import com.hady.saas.domain.indicacao.Indicacao;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Item 17 — indicação/referral automatizada. Consulta no painel.
 *
 * Gating: mesmo padrão do resto do robô/marketing seria `!= Plano.STANDARD`,
 * mas aqui não é necessário checar explicitamente — a função só gera dados
 * se Estabelecimento.indicacaoAutomaticaAtiva estiver ligada, e essa
 * configuração fica atrás do mesmo painel de configurações de
 * marketing/robô já gateado em outro lugar.
 */
@RestController
@RequestMapping("/painel/indicacoes")
public class IndicacaoPainelController {

    private final IndicacaoService indicacaoService;
    private final AuthUtil authUtil;

    public IndicacaoPainelController(IndicacaoService indicacaoService, AuthUtil authUtil) {
        this.indicacaoService = indicacaoService;
        this.authUtil = authUtil;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listar(Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        List<Indicacao> indicacoes = indicacaoService.listarPorEstabelecimento(estabelecimentoId);

        return ResponseEntity.ok(Map.of(
                "total", indicacoes.size(),
                "indicacoes", indicacoes.stream().map(i -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", i.getId());
                    m.put("status", i.getStatus().name());
                    m.put("indicador", i.getIndicador().getNome());
                    m.put("indicado", i.getIndicado() != null ? i.getIndicado().getNome() : null);
                    m.put("criadoEm", i.getCriadoEm().toString());
                    m.put("dataExpiracao", i.getDataExpiracao().toString());
                    m.put("concluidaEm", i.getConcluidaEm() != null ? i.getConcluidaEm().toString() : null);
                    m.put("cupomIndicadorCodigo", i.getCupomIndicador() != null ? i.getCupomIndicador().getCodigo() : null);
                    m.put("cupomIndicadoCodigo", i.getCupomIndicado() != null ? i.getCupomIndicado().getCodigo() : null);
                    return m;
                }).toList()
        ));
    }
}