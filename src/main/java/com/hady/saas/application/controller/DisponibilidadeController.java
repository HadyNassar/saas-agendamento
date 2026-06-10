package com.hady.saas.application.controller;

import com.hady.saas.business.service.AgendamentoService;
import com.hady.saas.business.service.ServicoService;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/painel/disponibilidade")
public class DisponibilidadeController {

    private final AgendamentoService agendamentoService;
    private final ServicoService servicoService;
    private final AuthUtil authUtil;

    public DisponibilidadeController(AgendamentoService agendamentoService,
                                     ServicoService servicoService,
                                     AuthUtil authUtil) {
        this.agendamentoService = agendamentoService;
        this.servicoService = servicoService;
        this.authUtil = authUtil;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<List<LocalTime>> disponibilidade(
            @RequestParam Long profissionalId,
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            Authentication auth) {

        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        Servico servico = servicoService.buscarPorId(servicoId, estabelecimentoId);

        List<LocalTime> slots = agendamentoService.gerarSlotsDisponiveis(
                profissionalId, estabelecimentoId, dia, servico.getDuracaoMinutos());

        return ResponseEntity.ok(slots);
    }
}