package com.hady.saas.application.controller;

import com.hady.saas.application.dto.AgendamentoPainelDTO;
import com.hady.saas.application.dto.AgendamentoResponseDTO;
import com.hady.saas.business.service.*;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.domain.usuario.Usuario;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/painel/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final UsuarioService usuarioService;
    private final ClienteService clienteService;
    private final ProfissionalService profissionalService;
    private final ServicoService servicoService;

    public AgendamentoController(AgendamentoService agendamentoService,
                                  UsuarioService usuarioService,
                                  ClienteService clienteService,
                                  ProfissionalService profissionalService,
                                  ServicoService servicoService) {
        this.agendamentoService = agendamentoService;
        this.usuarioService = usuarioService;
        this.clienteService = clienteService;
        this.profissionalService = profissionalService;
        this.servicoService = servicoService;
    }

    // GET /painel/agendamentos?dia=2026-05-25
    @GetMapping
    public ResponseEntity<List<AgendamentoResponseDTO>> listarPorDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        Long estabelecimentoId = usuario.getEstabelecimento().getId();

        List<Agendamento> agendamentos;

        if (usuario.getRole().name().equals("FUNCIONARIO_TOP")) {
            Profissional profissional = profissionalService.buscarPorUsuario(usuario.getId());
            agendamentos = agendamentoService.listarPorProfissionalEDia(profissional.getId(), dia);
        } else {
            agendamentos = agendamentoService.listarPorDia(estabelecimentoId, dia);
        }

        // converte para DTO — resolve o problema do FetchType.LAZY
        List<AgendamentoResponseDTO> dtos = agendamentos.stream()
                .map(AgendamentoResponseDTO::de)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // POST /painel/agendamentos
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO', 'FUNCIONARIO_TOP')")
    public ResponseEntity<String> agendar(@Valid @RequestBody AgendamentoPainelDTO dto,
                                           @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        Estabelecimento estabelecimento = usuario.getEstabelecimento();

        Cliente cliente = clienteService.buscarPorId(dto.getClienteId(), estabelecimento.getId());
        Profissional profissional = profissionalService.buscarPorId(dto.getProfissionalId(), estabelecimento.getId());
        Servico servico = servicoService.buscarPorId(dto.getServicoId(), estabelecimento.getId());

        agendamentoService.agendarPeloPainel(cliente, profissional, servico, estabelecimento, dto.getDataHoraInicio());

        return ResponseEntity.ok("Agendamento criado com sucesso!");
    }

    // PATCH /painel/agendamentos/{id}/cancelar
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<String> cancelar(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        agendamentoService.cancelar(id, usuario.getEstabelecimento().getId());
        return ResponseEntity.ok("Agendamento cancelado!");
    }

    // PATCH /painel/agendamentos/{id}/concluir
    @PatchMapping("/{id}/concluir")
    public ResponseEntity<String> concluir(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        agendamentoService.concluir(id, usuario.getEstabelecimento().getId());
        return ResponseEntity.ok("Atendimento concluído!");
    }

    // PATCH /painel/agendamentos/{id}/falta
    @PatchMapping("/{id}/falta")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<String> marcarFalta(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        agendamentoService.marcarFalta(id, usuario.getEstabelecimento().getId());
        return ResponseEntity.ok("Falta registrada!");
    }
}
