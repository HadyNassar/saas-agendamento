package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.cupom.CupomEstabelecimento;
import com.hady.saas.domain.enums.StatusIndicacao;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.indicacao.Indicacao;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.IndicacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Item 17 — indicação/referral automatizada.
 *
 * Regras confirmadas com o dono do produto (27/07/2026):
 *   - Opt-in por estabelecimento (Estabelecimento.indicacaoAutomaticaAtiva),
 *     mesmo padrão do cupom de reativação — tipo/valor de desconto
 *     configuráveis pelo dono.
 *   - Os DOIS ganham cupom: indicador e indicado.
 *   - Cupom só é liberado quando o indicado CONCLUI o primeiro atendimento
 *     (não no agendamento, não no cadastro).
 *   - Link único por cliente, validade de 15 dias a partir da geração.
 *   - Canal: WhatsApp simples (reaproveita WhatsAppService), não o bot
 *     conversacional — decisão técnica, ver enviarAgradecimentoNotaAlta.
 */
@Service
public class IndicacaoService {

    private static final Logger log = LoggerFactory.getLogger(IndicacaoService.class);

    private static final int DIAS_VALIDADE_LINK = 15;
    private static final List<StatusIndicacao> STATUS_ABERTOS =
            List.of(StatusIndicacao.AGUARDANDO_USO, StatusIndicacao.AGUARDANDO_PRIMEIRO_ATENDIMENTO);

    private final IndicacaoRepository indicacaoRepository;
    private final CupomEstabelecimentoService cupomEstabelecimentoService;
    private final ClienteService clienteService;
    private final AgendamentoRepository agendamentoRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public IndicacaoService(IndicacaoRepository indicacaoRepository,
                            CupomEstabelecimentoService cupomEstabelecimentoService,
                            ClienteService clienteService,
                            AgendamentoRepository agendamentoRepository) {
        this.indicacaoRepository = indicacaoRepository;
        this.cupomEstabelecimentoService = cupomEstabelecimentoService;
        this.clienteService = clienteService;
        this.agendamentoRepository = agendamentoRepository;
    }

    // ─── Geração do link (chamado pelo gatilho de nota alta) ─────────────────
    // Retorna null se a função não estiver ativa pro estabelecimento — quem
    // chama decide o que fazer (não injeta link nenhum na mensagem).

    @Transactional
    public String gerarLinkIndicacaoOuNull(Estabelecimento estabelecimento, Cliente indicador) {
        if (!estabelecimento.isIndicacaoAutomaticaAtiva()) return null;
        if (indicador == null) return null;

        try {
            Indicacao indicacao = new Indicacao();
            indicacao.setEstabelecimento(estabelecimento);
            indicacao.setIndicador(indicador);
            indicacao.setCodigo(gerarCodigoUnico());
            indicacao.setStatus(StatusIndicacao.AGUARDANDO_USO);
            indicacao.setDataExpiracao(LocalDateTime.now().plusDays(DIAS_VALIDADE_LINK));
            indicacao = indicacaoRepository.save(indicacao);

            return baseUrl + "/app/" + estabelecimento.getSlug() + "/indicar/" + indicacao.getCodigo();
        } catch (Exception e) {
            log.error("[Indicacao] Falha ao gerar link de indicação pro cliente id={}: {}",
                    indicador.getId(), e.getMessage(), e);
            return null;
        }
    }

    private String gerarCodigoUnico() {
        // UUID curto é suficiente — não precisa ser "bonito" como cupom
        // (esse código vai só na URL, não é digitado pelo cliente).
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    // ─── Verificação somente-leitura (tela pública antes de confirmar) ───────

    @Transactional(readOnly = true)
    public boolean linkValido(String codigo, Estabelecimento estabelecimento) {
        return indicacaoRepository.findByCodigo(codigo)
                .filter(i -> i.getEstabelecimento().getId().equals(estabelecimento.getId()))
                .filter(i -> i.getStatus() == StatusIndicacao.AGUARDANDO_USO)
                .filter(i -> i.getDataExpiracao().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    // ─── Uso do link (indicado abre) ──────────────────────────────────────────
    // GET /app/{slug}/indicar/{codigo}. Identifica/cria o Cliente indicado
    // (mesmo padrão de ClienteService.buscarOuCriar usado no agendamento
    // público) e vincula à indicação. Não cria agendamento aqui — só
    // registra a intenção; agendamentoReferencia é setado no primeiro
    // agendamento que o indicado de fato fizer (ver vincularAgendamentoSeAplicavel).

    public static class IndicacaoInvalidaException extends RuntimeException {
        public IndicacaoInvalidaException(String msg) { super(msg); }
    }

    @Transactional
    public Indicacao usarLink(String codigo, String telefoneIndicado, String nomeIndicado,
                              Estabelecimento estabelecimento) {
        Indicacao indicacao = indicacaoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IndicacaoInvalidaException("Link de indicação não encontrado."));

        if (!indicacao.getEstabelecimento().getId().equals(estabelecimento.getId())) {
            throw new IndicacaoInvalidaException("Link de indicação não encontrado.");
        }
        if (indicacao.getStatus() != StatusIndicacao.AGUARDANDO_USO) {
            throw new IndicacaoInvalidaException("Este link de indicação já foi usado ou expirou.");
        }
        if (indicacao.getDataExpiracao().isBefore(LocalDateTime.now())) {
            indicacao.setStatus(StatusIndicacao.EXPIRADA);
            indicacaoRepository.save(indicacao);
            throw new IndicacaoInvalidaException("Este link de indicação expirou.");
        }

        Cliente indicado = clienteService.buscarOuCriar(telefoneIndicado, nomeIndicado, null, estabelecimento);

        if (indicado.getId().equals(indicacao.getIndicador().getId())) {
            throw new IndicacaoInvalidaException("Você não pode usar seu próprio link de indicação.");
        }

        // Indicação é pra trazer gente nova — cliente que já teve um
        // atendimento concluído aqui não pode "se indicar" via outro
        // contato pra ganhar cupom de novo.
        if (agendamentoRepository.clienteJaTeveAtendimentoConcluido(indicado.getId())) {
            throw new IndicacaoInvalidaException(
                    "Este link é válido apenas para clientes novos, que ainda não foram atendidos aqui.");
        }

        indicacao.setIndicado(indicado);
        indicacao.setUsadoEm(LocalDateTime.now());
        indicacao.setStatus(StatusIndicacao.AGUARDANDO_PRIMEIRO_ATENDIMENTO);
        return indicacaoRepository.save(indicacao);
    }

    // ─── Gatilho de conclusão (chamado por AgendamentoService.concluir) ──────
    // Se o cliente do agendamento concluído é indicado de uma indicação ainda
    // pendente, libera os dois cupons e marca CONCLUIDA. Não faz nada se não
    // houver indicação pendente pra esse cliente — caminho normal na imensa
    // maioria dos agendamentos.

    @Transactional
    public void processarConclusaoSeAplicavel(Agendamento agendamentoConcluido) {
        Cliente cliente = agendamentoConcluido.getCliente();
        if (cliente == null) return;

        Optional<Indicacao> pendente = indicacaoRepository.findByIndicadoIdAndStatus(
                cliente.getId(), StatusIndicacao.AGUARDANDO_PRIMEIRO_ATENDIMENTO);
        if (pendente.isEmpty()) return;

        Indicacao indicacao = pendente.get();

        if (indicacao.getDataExpiracao().isBefore(LocalDateTime.now())) {
            indicacao.setStatus(StatusIndicacao.EXPIRADA);
            indicacaoRepository.save(indicacao);
            return;
        }

        Estabelecimento estabelecimento = indicacao.getEstabelecimento();
        if (!estabelecimento.isIndicacaoAutomaticaAtiva()) {
            // Dono desligou a função depois de o link já ter sido usado —
            // não gera cupom, mas fecha a indicação como concluída (o
            // primeiro atendimento de fato aconteceu).
            indicacao.setAgendamentoReferencia(agendamentoConcluido);
            indicacao.setStatus(StatusIndicacao.CONCLUIDA);
            indicacao.setConcluidaEm(LocalDateTime.now());
            indicacaoRepository.save(indicacao);
            return;
        }

        try {
            CupomEstabelecimento cupomIndicador = criarCupomPessoal(
                    estabelecimento, indicacao.getIndicador(), "INDIC");
            CupomEstabelecimento cupomIndicado = criarCupomPessoal(
                    estabelecimento, indicacao.getIndicado(), "BEMVINDO");

            indicacao.setCupomIndicador(cupomIndicador);
            indicacao.setCupomIndicado(cupomIndicado);
            indicacao.setAgendamentoReferencia(agendamentoConcluido);
            indicacao.setStatus(StatusIndicacao.CONCLUIDA);
            indicacao.setConcluidaEm(LocalDateTime.now());
            indicacaoRepository.save(indicacao);
        } catch (Exception e) {
            log.error("[Indicacao] Falha ao gerar cupons de indicação id={}: {}",
                    indicacao.getId(), e.getMessage(), e);
            // Não propaga — não pode impedir a conclusão do agendamento em si.
        }
    }

    private CupomEstabelecimento criarCupomPessoal(Estabelecimento estabelecimento, Cliente cliente, String prefixo) {
        String codigo = prefixo + cliente.getId() + "-" + System.currentTimeMillis() % 100000;
        return cupomEstabelecimentoService.criar(
                estabelecimento,
                codigo,
                estabelecimento.getIndicacaoTipoDesconto(),
                estabelecimento.getIndicacaoValorDesconto(),
                LocalDate.now().plusDays(30), // validade do cupom em si (não confundir com validade do link)
                1 // uso único — cupom pessoal
        );
    }

    // ─── Consulta (painel) ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Indicacao> listarPorEstabelecimento(Long estabelecimentoId) {
        return indicacaoRepository.findByEstabelecimentoIdOrderByCriadoEmDesc(estabelecimentoId);
    }

    // ─── Job de expiração ──────────────────────────────────────────────────

    @Transactional
    public int expirarPendentesVencidas() {
        List<Indicacao> expiradas = indicacaoRepository
                .findExpiradasNaoProcessadas(STATUS_ABERTOS, LocalDateTime.now());

        for (Indicacao i : expiradas) {
            i.setStatus(StatusIndicacao.EXPIRADA);
            indicacaoRepository.save(i);
        }
        return expiradas.size();
    }
}