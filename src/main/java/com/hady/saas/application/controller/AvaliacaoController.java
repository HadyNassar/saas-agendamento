package com.hady.saas.application.controller;

import com.hady.saas.business.service.IndicacaoService;
import com.hady.saas.business.service.WhatsAppService;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.alerta.AlertaPainel;
import com.hady.saas.domain.avaliacao.AvaliacaoAtendimento;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.enums.StatusAgendamento;
import com.hady.saas.domain.enums.TipoAlerta;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.AlertaPainelRepository;
import com.hady.saas.infrastructure.repository.AvaliacaoRepository;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints para avaliação pós-atendimento.
 *
 * Públicos (sem JWT):
 *   GET  /app/{slug}/avaliacao/{agendamentoId}       → verifica se já avaliou
 *   POST /app/{slug}/avaliacao/{agendamentoId}        → registra avaliação (1-5 + comentário)
 *   GET  /app/{slug}/avaliacoes                       → lista avaliações públicas do estabelecimento
 *   GET  /app/{slug}/avaliacoes/pendente/{clienteId}  → verifica se cliente tem avaliação pendente (pop-up)
 *
 * Autenticados (JWT) — apenas ELITE e PLATINUM:
 *   GET    /painel/avaliacoes                  → lista todas (incluindo ocultas) + média
 *   PATCH  /painel/avaliacoes/{id}/ocultar     → oculta ou exibe avaliação
 *   DELETE /painel/avaliacoes/{id}             → exclui avaliação permanentemente
 */
@RestController
public class AvaliacaoController {

    private final AgendamentoRepository agendamentoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AlertaPainelRepository alertaPainelRepository;
    private final AuthUtil authUtil;
    private final WhatsAppService whatsAppService;
    private final IndicacaoService indicacaoService;

    public AvaliacaoController(AgendamentoRepository agendamentoRepository,
                               AvaliacaoRepository avaliacaoRepository,
                               EstabelecimentoRepository estabelecimentoRepository,
                               AlertaPainelRepository alertaPainelRepository,
                               AuthUtil authUtil,
                               @Lazy WhatsAppService whatsAppService,
                               IndicacaoService indicacaoService) {
        this.agendamentoRepository = agendamentoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.alertaPainelRepository = alertaPainelRepository;
        this.authUtil = authUtil;
        this.whatsAppService = whatsAppService;
        this.indicacaoService = indicacaoService;
    }

    // ── GET /app/{slug}/avaliacao/{agendamentoId} ─────────────────────────────
    // Verifica se o agendamento já foi avaliado e retorna dados para montar a tela

    @GetMapping("/app/{slug}/avaliacao/{agendamentoId}")
    public ResponseEntity<Map<String, Object>> verificar(
            @PathVariable String slug,
            @PathVariable Long agendamentoId) {

        Agendamento agendamento = buscarAgendamento(slug, agendamentoId);
        boolean jaAvaliado = avaliacaoRepository.existsByAgendamentoId(agendamentoId);

        return ResponseEntity.ok(Map.of(
                "jaAvaliado",      jaAvaliado,
                "estabelecimento", agendamento.getEstabelecimento().getNome(),
                "servico",         agendamento.getServico().getNome(),
                "profissional",    agendamento.getProfissional().getNome()
        ));
    }

    // ── POST /app/{slug}/avaliacao/{agendamentoId} ────────────────────────────
    // Cliente registra a avaliação (nota + comentário opcional)

    @PostMapping("/app/{slug}/avaliacao/{agendamentoId}")
    public ResponseEntity<String> avaliar(
            @PathVariable String slug,
            @PathVariable Long agendamentoId,
            @Valid @RequestBody AvaliacaoRequestDTO dto) {

        // FIX: @Valid adicionado — as anotações @NotNull @Min(1) @Max(5) do DTO
        // eram silenciosamente ignoradas sem ele. O GlobalExceptionHandler trata
        // MethodArgumentNotValidException com 400 uniforme.
        // Validação manual removida — era duplicata do @Valid.

        Agendamento agendamento = buscarAgendamento(slug, agendamentoId);

        if (agendamento.getStatus() != StatusAgendamento.CONCLUIDO) {
            return ResponseEntity.badRequest()
                    .body("Apenas atendimentos concluídos podem ser avaliados.");
        }

        if (avaliacaoRepository.existsByAgendamentoId(agendamentoId)) {
            return ResponseEntity.badRequest().body("Este atendimento já foi avaliado.");
        }

        AvaliacaoAtendimento avaliacao = new AvaliacaoAtendimento();
        avaliacao.setAgendamento(agendamento);
        avaliacao.setCliente(agendamento.getCliente());
        avaliacao.setEstabelecimento(agendamento.getEstabelecimento());
        avaliacao.setNota(dto.getNota());
        avaliacao.setComentario(dto.getComentario());
        avaliacaoRepository.save(avaliacao);

        // Item 16 — gatilho de reputação: reage à nota assim que ela é registrada.
        // Nota 5 → agradecimento simples via WhatsApp (sem link de review externo,
        // não existe página de review público fora do app hoje).
        // Nota 1 ou 2 → alerta no painel para o dono, sem mensagem de WhatsApp.
        if (dto.getNota() == 5) {
            String linkIndicacao = indicacaoService.gerarLinkIndicacaoOuNull(
                    agendamento.getEstabelecimento(), agendamento.getCliente());
            whatsAppService.enviarAgradecimentoNotaAlta(agendamento, linkIndicacao);
        } else if (dto.getNota() <= 2) {
            AlertaPainel alerta = new AlertaPainel();
            alerta.setEstabelecimento(agendamento.getEstabelecimento());
            alerta.setTipo(TipoAlerta.AVALIACAO_NOTA_BAIXA);
            alerta.setReferenciaId(avaliacao.getId());
            alerta.setMensagem(String.format(
                    "%s deu nota %d na avaliação do atendimento com %s (%s).",
                    agendamento.getCliente().getNome(),
                    dto.getNota(),
                    agendamento.getProfissional().getNome(),
                    agendamento.getServico().getNome()
            ));
            alertaPainelRepository.save(alerta);
        }

        return ResponseEntity.ok("Avaliação registrada! Obrigado pelo feedback 😊");
    }

    // ── GET /app/{slug}/avaliacoes ────────────────────────────────────────────
    // Lista avaliações públicas (visíveis) do estabelecimento para a página pública

    @GetMapping("/app/{slug}/avaliacoes")
    public ResponseEntity<Map<String, Object>> listarPublicas(@PathVariable String slug) {

        Estabelecimento estab = estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        // Só exibe avaliações públicas para planos != STANDARD (ELITE e
        // PLATINUM são idênticos em funcionalidade — padronizado 27/07/2026,
        // mesmo comportamento de antes, só a forma de checar mudou).
        if (estab.getPlano() == Plano.STANDARD) {
            return ResponseEntity.ok(Map.of(
                    "media", 0.0,
                    "total", 0,
                    "avaliacoes", List.of()
            ));
        }

        List<AvaliacaoAtendimento> avaliacoes =
                avaliacaoRepository.findVisivelByEstabelecimentoId(estab.getId());

        Double media = avaliacaoRepository.mediaNotasEstabelecimento(estab.getId());

        return ResponseEntity.ok(Map.of(
                "media",      media != null ? Math.round(media * 10.0) / 10.0 : 0.0,
                "total",      avaliacoes.size(),
                "avaliacoes", avaliacoes.stream().map(a -> Map.of(
                        "id",          a.getId(),
                        "nota",        a.getNota(),
                        "comentario",  a.getComentario() != null ? a.getComentario() : "",
                        "cliente",     a.getCliente().getNome().split(" ")[0], // só o primeiro nome
                        "servico",     a.getAgendamento().getServico().getNome(),
                        "profissional",a.getAgendamento().getProfissional().getNome(),
                        "criadoEm",    a.getCriadoEm().toString()
                )).toList()
        ));
    }

    // ── GET /app/{slug}/avaliacoes/pendente/{clienteId} ───────────────────────
    // Verifica se o cliente tem alguma avaliação pendente (para mostrar pop-up)

    @GetMapping("/app/{slug}/avaliacoes/pendente/{clienteId}")
    public ResponseEntity<Map<String, Object>> verificarPendente(
            @PathVariable String slug,
            @PathVariable Long clienteId) {

        Estabelecimento estab = estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        boolean temPendente = avaliacaoRepository
                .clienteTemAvaliacaoPendente(clienteId, estab.getId());

        return ResponseEntity.ok(Map.of("temAvaliacaoPendente", temPendente));
    }

    // ── GET /painel/avaliacoes ────────────────────────────────────────────────
    // Lista todas as avaliações no painel (incluindo ocultas) — ELITE e PLATINUM

    @GetMapping("/painel/avaliacoes")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listar(Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        validarPlano(estabelecimentoId);

        List<AvaliacaoAtendimento> avaliacoes =
                avaliacaoRepository.findByEstabelecimentoIdOrderByCriadoEmDesc(estabelecimentoId);

        Double media = avaliacaoRepository.mediaNotasEstabelecimento(estabelecimentoId);

        return ResponseEntity.ok(Map.of(
                "media",      media != null ? Math.round(media * 10.0) / 10.0 : 0.0,
                "total",      avaliacoes.size(),
                "avaliacoes", avaliacoes.stream().map(a -> Map.of(
                        "id",          a.getId(),
                        "nota",        a.getNota(),
                        "comentario",  a.getComentario() != null ? a.getComentario() : "",
                        "oculta",      a.isOculta(),
                        "cliente",     a.getCliente().getNome(),
                        "servico",     a.getAgendamento().getServico().getNome(),
                        "profissional",a.getAgendamento().getProfissional().getNome(),
                        "criadoEm",    a.getCriadoEm().toString()
                )).toList()
        ));
    }

    // ── PATCH /painel/avaliacoes/{id}/ocultar ─────────────────────────────────
    // Oculta ou exibe uma avaliação na página pública — ELITE e PLATINUM

    @PatchMapping("/painel/avaliacoes/{id}/ocultar")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> toggleOcultar(
            @PathVariable Long id,
            Authentication auth) {

        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        validarPlano(estabelecimentoId);

        AvaliacaoAtendimento avaliacao = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));

        if (!avaliacao.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Avaliação não pertence a este estabelecimento");
        }

        avaliacao.setOculta(!avaliacao.isOculta());
        avaliacaoRepository.save(avaliacao);

        String msg = avaliacao.isOculta() ? "Avaliação ocultada da página pública." : "Avaliação exibida na página pública.";
        return ResponseEntity.ok(msg);
    }

    // ── DELETE /painel/avaliacoes/{id} ────────────────────────────────────────
    // Exclui avaliação permanentemente — ELITE e PLATINUM

    @DeleteMapping("/painel/avaliacoes/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deletar(
            @PathVariable Long id,
            Authentication auth) {

        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        validarPlano(estabelecimentoId);

        AvaliacaoAtendimento avaliacao = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));

        if (!avaliacao.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Avaliação não pertence a este estabelecimento");
        }

        avaliacaoRepository.delete(avaliacao);
        return ResponseEntity.ok("Avaliação excluída com sucesso.");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Agendamento buscarAgendamento(String slug, Long agendamentoId) {
        return agendamentoRepository.findByIdWithDetails(agendamentoId)
                .filter(a -> a.getEstabelecimento().getSlug().equals(slug))
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));
    }

    private void validarPlano(Long estabelecimentoId) {
        Estabelecimento estab = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        if (estab.getPlano() == Plano.STANDARD) {
            throw new RuntimeException("Avaliações de clientes disponíveis nos planos Elite e Platinum.");
        }
    }

    // ─── DTO ──────────────────────────────────────────────────────────────────

    public static class AvaliacaoRequestDTO {
        @NotNull
        @Min(1) @Max(5)
        private Integer nota;
        private String comentario;

        public Integer getNota()            { return nota; }
        public String getComentario()       { return comentario; }
        public void setNota(Integer nota)   { this.nota = nota; }
        public void setComentario(String c) { this.comentario = c; }
    }
}