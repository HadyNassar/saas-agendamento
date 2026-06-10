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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // Retorna todos os slots com disponivel: true/false
    // O frontend usa isso para mostrar ocupados em vermelho e disponíveis em verde
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<List<Map<String, Object>>> disponibilidade(
            @RequestParam Long profissionalId,
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            Authentication auth) {

        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        Servico servico = servicoService.buscarPorId(servicoId, estabelecimentoId);

        // Slots livres
        List<LocalTime> livres = agendamentoService.gerarSlotsDisponiveis(
                profissionalId, estabelecimentoId, dia, servico.getDuracaoMinutos());

        // Todos os slots (livres + ocupados)
        List<LocalTime> todos = agendamentoService.gerarTodosSlots(
                profissionalId, estabelecimentoId, dia, servico.getDuracaoMinutos());

        List<Map<String, Object>> resultado = todos.stream().map(slot -> {
            Map<String, Object> m = new HashMap<>();
            m.put("horario", slot.toString());
            m.put("disponivel", livres.contains(slot));
            return m;
        }).toList();

        return ResponseEntity.ok(resultado);
    }
}