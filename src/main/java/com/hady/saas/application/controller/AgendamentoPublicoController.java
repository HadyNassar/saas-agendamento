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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // GET /app/{slug}/configuracoes
    @GetMapping("/configuracoes")
    public ResponseEntity<Map<String, Object>> configuracoesPublicas(@PathVariable String slug) {
        Estabelecimento est = buscarEstabelecimento(slug);
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("nome", est.getNome());
        cfg.put("mesesAntecipacao", est.getMesesAntecipacao());
        // ── campos de pagamento/sinal necessários pelo frontend público ──
        cfg.put("aceitaPagamento", est.isAceitaPagamento());
        cfg.put("exigirSinal", est.isExigirSinal());
        cfg.put("tipoSinal", est.getTipoSinal());
        cfg.put("valorSinal", est.getValorSinal());
        return ResponseEntity.ok(cfg);
    }

    // GET /app/{slug}/profissionais
    @GetMapping("/profissionais")
    public ResponseEntity<?> listarProfissionais(@PathVariable String slug) {
        Estabelecimento estabelecimento = buscarEstabelecimento(slug);
        return ResponseEntity.ok(profissionalService.listarAtivos(estabelecimento.getId()));
    }

    // GET /app/{slug}/servicos
    // Aceita ?profissionalId={id} opcional.
    // Com profissionalId: retorna só os serviços que AQUELE profissional realiza (relação N×N).
    // Sem profissionalId: fallback — retorna todos os serviços ativos (compatibilidade).
    @GetMapping("/servicos")
    public ResponseEntity<?> listarServicos(
            @PathVariable String slug,
            @RequestParam(required = false) Long profissionalId) {

        Estabelecimento estabelecimento = buscarEstabelecimento(slug);

        if (profissionalId != null) {
            // N×N: o mesmo serviço pode aparecer para vários profissionais;
            // aqui filtramos só os que ESTE profissional sabe fazer
            return ResponseEntity.ok(
                    profissionalService.listarServicosDoProfissional(profissionalId, estabelecimento.getId())
            );
        }

        // Fallback: todos os serviços ativos do estabelecimento
        return ResponseEntity.ok(servicoService.listarAtivos(estabelecimento.getId()));
    }

    // GET /app/{slug}/disponibilidade — retorna apenas slots LIVRES (mantido para compatibilidade)
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

    // GET /app/{slug}/todos-slots — retorna TODOS os slots com disponivel: true/false
    // Usado pelo calendário visual para mostrar ocupados em vermelho
    @GetMapping("/todos-slots")
    public ResponseEntity<List<Map<String, Object>>> todosSlots(
            @PathVariable String slug,
            @RequestParam Long profissionalId,
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia) {

        Estabelecimento estabelecimento = buscarEstabelecimento(slug);
        Servico servico = servicoService.buscarPorId(servicoId, estabelecimento.getId());

        // slots livres
        List<LocalTime> livres = agendamentoService.gerarSlotsDisponiveis(
                profissionalId, estabelecimento.getId(), dia, servico.getDuracaoMinutos());

        // todos os slots (livres + ocupados)
        List<LocalTime> todos = agendamentoService.gerarTodosSlots(
                profissionalId, estabelecimento.getId(), dia, servico.getDuracaoMinutos());

        List<Map<String, Object>> resultado = todos.stream().map(slot -> {
            Map<String, Object> m = new HashMap<>();
            m.put("horario", slot.toString());
            m.put("disponivel", livres.contains(slot));
            return m;
        }).toList();

        return ResponseEntity.ok(resultado);
    }

    // POST /app/{slug}/agendar
    @PostMapping("/agendar")
    public ResponseEntity<Map<String, Object>> agendar(@PathVariable String slug,
                                                       @Valid @RequestBody AgendamentoPublicoDTO dto) {

        Estabelecimento estabelecimento = buscarEstabelecimento(slug);

        Cliente cliente = clienteService.buscarOuCriar(
                dto.getClienteTelefone(),
                dto.getClienteNome(),
                dto.getClienteEmail(),
                estabelecimento
        );

        Profissional profissional = profissionalService.buscarPorId(dto.getProfissionalId(), estabelecimento.getId());
        Servico servico = servicoService.buscarPorId(dto.getServicoId(), estabelecimento.getId());

        if (!estabelecimento.temAcesso()) {
            throw new RuntimeException("Este estabelecimento não está aceitando agendamentos no momento.");
        }

        var agendamento = agendamentoService.agendarPeloCliente(
                cliente, profissional, servico, estabelecimento,
                dto.getDataHoraInicio(), dto.isAguardandoPagamento()
        );

        return ResponseEntity.ok(Map.of(
                "agendamentoId", agendamento.getId(),
                "mensagem", "Agendamento realizado com sucesso!"
        ));
    }

    private Estabelecimento buscarEstabelecimento(String slug) {
        return estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }
}