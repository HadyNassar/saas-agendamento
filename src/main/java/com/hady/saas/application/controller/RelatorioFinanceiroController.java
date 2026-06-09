package com.hady.saas.application.controller;

import com.hady.saas.application.dto.RelatorioFinanceiroDTO;
import com.hady.saas.business.service.RelatorioFinanceiroService;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/painel/relatorio")
public class RelatorioFinanceiroController {

    private static final long MAX_DIAS = 366;

    private final RelatorioFinanceiroService relatorioService;
    private final AuthUtil authUtil;

    public RelatorioFinanceiroController(RelatorioFinanceiroService relatorioService, AuthUtil authUtil) {
        this.relatorioService = relatorioService;
        this.authUtil = authUtil;
    }

    // Restrito a ADMIN — dados financeiros (faturamento, comissões, lucro) são sigilosos.
    // A verificação de plano (ELITE/PLATINUM) é feita dentro do service.
    @GetMapping("/financeiro")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRelatorio(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication auth) {

        if (inicio.isAfter(fim)) {
            return ResponseEntity.badRequest().body("A data de início não pode ser posterior à data de fim.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) > MAX_DIAS) {
            return ResponseEntity.badRequest().body("O intervalo máximo permitido é de " + MAX_DIAS + " dias.");
        }

        RelatorioFinanceiroDTO dto = relatorioService.gerarRelatorio(authUtil.getEstabelecimentoId(auth), inicio, fim);
        return ResponseEntity.ok(dto);
    }
}

