package com.hady.saas.application.controller;

import com.hady.saas.application.dto.AgendamentoPainelDTO;
import com.hady.saas.application.dto.AgendamentoResponseDTO;
import com.hady.saas.business.service.*;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import com.hady.saas.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/painel/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final ClienteService clienteService;
    private final ProfissionalService profissionalService;
    private final ServicoService servicoService;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AuthUtil authUtil;

    public AgendamentoController(AgendamentoService agendamentoService,
                                  ClienteService clienteService,
                                  ProfissionalService profissionalService,
                                  ServicoService servicoService,
                                  EstabelecimentoRepository estabelecimentoRepository,
                                  AuthUtil authUtil) {
        this.agendamentoService = agendamentoService;
        this.clienteService = clienteService;
        this.profissionalService = profissionalService;
        this.servicoService = servicoService;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.authUtil = authUtil;
    }

    @GetMapping
    public ResponseEntity<List<AgendamentoResponseDTO>> listarPorDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            Authentication auth) {

        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        List<Agendamento> agendamentos;

        // PROFISSIONAL só vê os próprios agendamentos
        boolean isFuncionarioPlatinum = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROFISSIONAL"));

        if (isFuncionarioPlatinum) {
            Profissional profissional = profissionalService.buscarPorUsuario(authUtil.getUsuarioId(auth));
            agendamentos = agendamentoService.listarPorProfissionalEDia(profissional.getId(), estabelecimentoId, dia);
        } else {
            agendamentos = agendamentoService.listarPorDia(estabelecimentoId, dia);
        }

        return ResponseEntity.ok(agendamentos.stream().map(AgendamentoResponseDTO::de).toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<String> agendar(@Valid @RequestBody AgendamentoPainelDTO dto,
                                           Authentication auth) {

        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();

        Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        // PROFISSIONAL só pode criar agendamentos na própria agenda.
        boolean isFuncionarioPlatinum = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROFISSIONAL"));

        if (isFuncionarioPlatinum) {
            Profissional proprio = profissionalService.buscarPorUsuario(authUtil.getUsuarioId(auth));
            if (!proprio.getId().equals(dto.getProfissionalId())) {
                throw new RuntimeException(
                        "No plano Platinum o funcionário só pode criar agendamentos na própria agenda.");
            }
        }

        Cliente cliente = clienteService.buscarPorId(dto.getClienteId(), estabelecimentoId);
        Profissional profissional = profissionalService.buscarPorId(dto.getProfissionalId(), estabelecimentoId);
        Servico servico = servicoService.buscarPorId(dto.getServicoId(), estabelecimentoId);

        agendamentoService.agendarPeloPainel(cliente, profissional, servico, estabelecimento, dto.getDataHoraInicio());
        return ResponseEntity.ok("Agendamento criado com sucesso!");
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<String> cancelar(@PathVariable Long id, Authentication auth) {
        validarAcessoAgendamento(id, auth);
        agendamentoService.cancelar(id, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.ok("Agendamento cancelado!");
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<String> concluir(@PathVariable Long id, Authentication auth) {
        validarAcessoAgendamento(id, auth);
        agendamentoService.concluir(id, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.ok("Atendimento concluído!");
    }

    @PatchMapping("/{id}/falta")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<String> marcarFalta(@PathVariable Long id, Authentication auth) {
        validarAcessoAgendamento(id, auth);
        agendamentoService.marcarFalta(id, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.ok("Falta registrada!");
    }

    // PROFISSIONAL só pode operar em agendamentos da própria agenda.
    // ADMIN e GERENTE podem operar em qualquer agendamento do estabelecimento.
    private void validarAcessoAgendamento(Long agendamentoId, Authentication auth) {
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        boolean isFuncionarioPlatinum = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROFISSIONAL"));

        if (isFuncionarioPlatinum) {
            Profissional proprio = profissionalService.buscarPorUsuario(authUtil.getUsuarioId(auth));
            Agendamento agendamento = agendamentoService.buscarPorId(
                    agendamentoId, authUtil.getEstabelecimentoId(auth));
            if (!agendamento.getProfissional().getId().equals(proprio.getId())) {
                throw new RuntimeException(
                        "No plano Platinum o funcionário só pode alterar agendamentos da própria agenda.");
            }
        }
    }
}
