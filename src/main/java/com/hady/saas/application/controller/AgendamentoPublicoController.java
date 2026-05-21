package com.hady.saas.application.controller;

import com.hady.saas.application.dto.AgendamentoPublicoDTO;
import com.hady.saas.business.service.AgendamentoService;
import com.hady.saas.business.service.ClienteService;
import com.hady.saas.business.service.ProfissionalService;
import com.hady.saas.business.service.ServicoService;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/app/{slug}")
public class AgendamentoPublicoController {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ProfissionalService profissionalService;
    private final ServicoService servicoService;
    private final ClienteService clienteService;
    private final AgendamentoService agendamentoService;

    public AgendamentoPublicoController(EstabelecimentoRepository estabelecimentoRepository,
                                         ProfissionalService profissionalService,
                                         ServicoService servicoService,
                                         ClienteService clienteService,
                                         AgendamentoService agendamentoService) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.profissionalService = profissionalService;
        this.servicoService = servicoService;
        this.clienteService = clienteService;
        this.agendamentoService = agendamentoService;
    }

    // GET /app/barbearia-do-joao/profissionais
    // Lista profissionais disponíveis (sem login)
    @GetMapping("/profissionais")
    public ResponseEntity<?> listarProfissionais(@PathVariable String slug) {
        Estabelecimento estabelecimento = buscarEstabelecimento(slug);
        return ResponseEntity.ok(profissionalService.listarAtivos(estabelecimento.getId()));
    }

    // GET /app/barbearia-do-joao/servicos
    // Lista serviços disponíveis (sem login)
    @GetMapping("/servicos")
    public ResponseEntity<?> listarServicos(@PathVariable String slug) {
        Estabelecimento estabelecimento = buscarEstabelecimento(slug);
        return ResponseEntity.ok(servicoService.listarAtivos(estabelecimento.getId()));
    }

    // GET /app/barbearia-do-joao/disponibilidade?profissionalId=1&servicoId=1&dia=2026-05-20
    // Retorna slots de horários disponíveis (sem login)
    @GetMapping("/disponibilidade")
    public ResponseEntity<List<LocalTime>> disponibilidade(
            @PathVariable String slug,
            @RequestParam Long profissionalId,
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia) {

        Estabelecimento estabelecimento = buscarEstabelecimento(slug);
        Servico servico = servicoService.buscarPorId(servicoId, estabelecimento.getId());

        List<LocalTime> slots = agendamentoService.gerarSlotsDisponiveis(
                profissionalId, estabelecimento.getId(), dia, servico.getDuracaoMinutos());

        return ResponseEntity.ok(slots);
    }

    // POST /app/barbearia-do-joao/agendar
    // Cliente agenda sem login
    @PostMapping("/agendar")
    public ResponseEntity<String> agendar(@PathVariable String slug,
                                           @Valid @RequestBody AgendamentoPublicoDTO dto) {

        Estabelecimento estabelecimento = buscarEstabelecimento(slug);

        // Busca ou cria cliente automaticamente pelo telefone
        Cliente cliente = clienteService.buscarOuCriar(
                dto.getClienteTelefone(),
                dto.getClienteNome(),
                dto.getClienteEmail(),
                estabelecimento
        );

        Profissional profissional = profissionalService.buscarPorId(dto.getProfissionalId(), estabelecimento.getId());
        Servico servico = servicoService.buscarPorId(dto.getServicoId(), estabelecimento.getId());

        agendamentoService.agendarPeloCliente(cliente, profissional, servico, estabelecimento, dto.getDataHoraInicio());

        return ResponseEntity.ok("Agendamento realizado com sucesso!");
    }

    private Estabelecimento buscarEstabelecimento(String slug) {
        return estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }
}
