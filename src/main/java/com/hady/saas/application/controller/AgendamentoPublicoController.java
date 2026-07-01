package com.hady.saas.application.controller;

import com.hady.saas.application.dto.AgendamentoPublicoDTO;
import com.hady.saas.application.dto.ProfissionalPublicoDTO;
import com.hady.saas.business.service.AgendamentoService;
import com.hady.saas.business.service.ClienteService;
import com.hady.saas.business.service.ProfissionalService;
import com.hady.saas.business.service.ServicoService;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.infrastructure.repository.AvaliacaoRepository;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
    private final AvaliacaoRepository avaliacaoRepository;

    public AgendamentoPublicoController(EstabelecimentoRepository estabelecimentoRepository,
                                        ProfissionalService profissionalService,
                                        ServicoService servicoService,
                                        ClienteService clienteService,
                                        AgendamentoService agendamentoService,
                                        AvaliacaoRepository avaliacaoRepository) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.profissionalService = profissionalService;
        this.servicoService = servicoService;
        this.clienteService = clienteService;
        this.agendamentoService = agendamentoService;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    // GET /app/{slug}/configuracoes
    // @Transactional(readOnly = true) — NECESSÁRIO: comodidades e formasPagamento
    // são @ElementCollection (lazy) na entidade Estabelecimento. Sem uma sessão
    // Hibernate aberta durante a serialização do JSON, o Jackson tenta ler esses
    // proxies fora da sessão e estoura LazyInitializationException (erro visto
    // em produção: "could not initialize proxy - no Session").
    @Transactional(readOnly = true)
    @GetMapping("/configuracoes")
    public ResponseEntity<Map<String, Object>> configuracoesPublicas(@PathVariable String slug) {
        Estabelecimento est = buscarEstabelecimento(slug);
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("nome", est.getNome());
        cfg.put("mesesAntecipacao", est.getMesesAntecipacao());
        cfg.put("aceitaPagamento", est.isAceitaPagamento());
        cfg.put("exigirSinal", est.isExigirSinal());
        cfg.put("tipoSinal", est.getTipoSinal());
        cfg.put("valorSinal", est.getValorSinal());
        // NOVO — faltavam na página pública: logo do estabelecimento (Problema A),
        // redes sociais e comodidades/formas de pagamento (Problema 2A/2B),
        // já expostos há tempos no endpoint autenticado /painel/configuracoes/estabelecimento
        // mas nunca replicados aqui, no endpoint público.
        cfg.put("fotoPerfil", est.getFotoPerfil());
        cfg.put("instagram", est.getInstagram());
        cfg.put("whatsappContato", est.getWhatsappContato());
        cfg.put("facebook", est.getFacebook());
        cfg.put("comodidades", est.getComodidades());
        cfg.put("formasPagamento", est.getFormasPagamento());
        return ResponseEntity.ok(cfg);
    }

    // GET /app/{slug}/profissionais
    @Transactional(readOnly = true)
    @GetMapping("/profissionais")
    public ResponseEntity<List<ProfissionalPublicoDTO>> listarProfissionais(@PathVariable String slug) {
        Estabelecimento estabelecimento = buscarEstabelecimento(slug);
        List<Profissional> profissionais = profissionalService.listarAtivos(estabelecimento.getId());
        List<ProfissionalPublicoDTO> dtos = profissionais.stream()
                .map(p -> {
                    Double media = avaliacaoRepository.mediaNotasProfissional(p.getId());
                    return ProfissionalPublicoDTO.de(p, media);
                })
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // GET /app/{slug}/servicos
    @GetMapping("/servicos")
    public ResponseEntity<?> listarServicos(
            @PathVariable String slug,
            @RequestParam(required = false) Long profissionalId) {

        Estabelecimento estabelecimento = buscarEstabelecimento(slug);

        if (profissionalId != null) {
            return ResponseEntity.ok(
                    profissionalService.listarServicosDoProfissional(profissionalId, estabelecimento.getId())
            );
        }

        return ResponseEntity.ok(servicoService.listarAtivos(estabelecimento.getId()));
    }

    // GET /app/{slug}/disponibilidade
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

    // GET /app/{slug}/todos-slots
    @GetMapping("/todos-slots")
    public ResponseEntity<List<Map<String, Object>>> todosSlots(
            @PathVariable String slug,
            @RequestParam Long profissionalId,
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia) {

        Estabelecimento estabelecimento = buscarEstabelecimento(slug);
        Servico servico = servicoService.buscarPorId(servicoId, estabelecimento.getId());

        List<LocalTime> livres = agendamentoService.gerarSlotsDisponiveis(
                profissionalId, estabelecimento.getId(), dia, servico.getDuracaoMinutos());

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
                "clienteId", cliente.getId(),
                "mensagem", "Agendamento realizado com sucesso!"
        ));
    }

    private Estabelecimento buscarEstabelecimento(String slug) {
        return estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }
}