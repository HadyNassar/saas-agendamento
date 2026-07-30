package com.hady.saas.business.service;

import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.cupom.CupomEstabelecimento;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.whatsapp.CampanhaEnvio;
import com.hady.saas.domain.whatsapp.CampanhaEnvioItem;
import com.hady.saas.domain.enums.EstagioCliente;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.CampanhaEnvioItemRepository;
import com.hady.saas.infrastructure.repository.CampanhaEnvioRepository;
import com.hady.saas.infrastructure.repository.ClienteRepository;
import com.hady.saas.infrastructure.repository.ProfissionalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * Regra de negócio da campanha de disparo em massa MANUAL (seção 5-B do
 * documento — o dono do estabelecimento dispara para um segmento de
 * clientes). Reaproveita a fundação técnica que já existia (CampanhaEnvio /
 * CampanhaEnvioItem / CampanhaEnvioProcessorJob).
 *
 * Ciclo de vida da campanha:
 *
 *   RASCUNHO -> (dono revisa e confirma) -> CRIADA -> (job processa em
 *   lotes) -> EM_ANDAMENTO -> CONCLUIDA / CONCLUIDA_COM_ERROS
 *                  \-> (dono desiste antes de confirmar) -> CANCELADA
 *
 * Os itens de um rascunho ficam com status RASCUNHO (não PENDENTE) — o
 * CampanhaEnvioProcessorJob só enxerga PENDENTE, então uma campanha em
 * rascunho é estruturalmente invisível pro job até a confirmação.
 *
 * Mitigações anti-banimento adicionadas nesta versão:
 *   1) Intervalo mínimo configurável entre campanhas
 *      (Estabelecimento.intervaloMinimoCampanhaDias), verificado na
 *      CONFIRMAÇÃO — não trava o dono de rascunhar/revisar antes da hora.
 *   2) Itens de uma mesma campanha são espalhados no tempo via
 *      agendadoPara, em lotes, em vez de todos ficarem PENDENTE ao mesmo
 *      tempo.
 *   3) Variação leve de saudação entre lotes, pra reduzir o padrão de
 *      "mensagem idêntica em massa".
 */
@Service
public class CampanhaEnvioService {

    private final CampanhaEnvioRepository campanhaRepository;
    private final CampanhaEnvioItemRepository itemRepository;
    private final ClienteRepository clienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final CupomEstabelecimentoService cupomEstabelecimentoService;

    // Mesmo padrão de link já usado por CampanhaAutomaticaService pro cupom
    // de reativação: baseUrl + /app/{slug}?cupom={codigo}.
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public CampanhaEnvioService(CampanhaEnvioRepository campanhaRepository,
                                CampanhaEnvioItemRepository itemRepository,
                                ClienteRepository clienteRepository,
                                ProfissionalRepository profissionalRepository,
                                AgendamentoRepository agendamentoRepository,
                                CupomEstabelecimentoService cupomEstabelecimentoService) {
        this.campanhaRepository = campanhaRepository;
        this.itemRepository = itemRepository;
        this.clienteRepository = clienteRepository;
        this.profissionalRepository = profissionalRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.cupomEstabelecimentoService = cupomEstabelecimentoService;
    }

    public static class CampanhaEnvioException extends RuntimeException {
        public CampanhaEnvioException(String mensagem) { super(mensagem); }
    }

    // Quantos destinatários compartilham o mesmo agendadoPara/variação de
    // saudação, dentro de uma mesma campanha.
    private static final int TAMANHO_LOTE_DISTRIBUICAO = 30;

    // Intervalo entre um lote e o próximo, dentro da MESMA campanha.
    private static final long MINUTOS_ENTRE_LOTES = 12;

    // Pequenas variações de abertura, alternadas por lote — escolha de
    // produto, não regra oficial da Meta/WhatsApp. O texto livre e o
    // template continuam os mesmos; só a saudação de abertura muda.
    private static final List<String> VARIACOES_SAUDACAO = List.of(
            "Olá",
            "Oi",
            "Olá, tudo bem?",
            "Oi, tudo bem?"
    );

    // ── Criação do rascunho ─────────────────────────────────────────────────

    @Transactional
    public CampanhaEnvio criarRascunho(Estabelecimento estabelecimento, EstagioCliente filtroEstagio, String mensagem) {
        return criarRascunho(estabelecimento, filtroEstagio, mensagem, null, null);
    }

    // profissionalIds: null/vazia = todos os profissionais (comportamento
    // padrão, preserva o método de 3 argumentos acima pra quem já usava).
    // cupomConfig: null = campanha sem cupom.
    @Transactional
    public CampanhaEnvio criarRascunho(Estabelecimento estabelecimento, EstagioCliente filtroEstagio, String mensagem,
                                       List<Long> profissionalIds, CupomConfig cupomConfig) {
        return criarRascunho(estabelecimento, filtroEstagio, mensagem, profissionalIds, cupomConfig, null);
    }

    // cupomExistenteId: quando informado, reaproveita um CupomEstabelecimento
    // já existente (criado manualmente na tela de Cupons) em vez de gerar um
    // novo — tem prioridade sobre cupomConfig se os dois vierem preenchidos
    // por engano (não deveria acontecer, o front manda só um dos dois).
    @Transactional
    public CampanhaEnvio criarRascunho(Estabelecimento estabelecimento, EstagioCliente filtroEstagio, String mensagem,
                                       List<Long> profissionalIds, CupomConfig cupomConfig, Long cupomExistenteId) {
        if (filtroEstagio == null) {
            throw new CampanhaEnvioException("Campo 'filtroEstagio' é obrigatório.");
        }
        if (mensagem == null || mensagem.isBlank()) {
            throw new CampanhaEnvioException("Campo 'mensagem' é obrigatório.");
        }

        List<Cliente> destinatarios;
        if (filtroEstagio == EstagioCliente.TODOS) {
            // Ignora estagioAtual completamente — pega todo cliente
            // cadastrado no estabelecimento, exceto bloqueados.
            destinatarios = clienteRepository.findByEstabelecimentoIdAndBloqueadoFalse(estabelecimento.getId());
        } else {
            // ATENÇÃO (comportamento pré-existente, não uma regra nova):
            // este filtro usa Cliente.estagioAtual, valor PERSISTIDO que só é
            // recalculado quando um agendamento do cliente é concluído (ver
            // ClienteSegmentacaoService). Cliente cadastrado manualmente ou
            // sem atendimento concluído tem estagioAtual = null e por isso
            // não aparece em NENHUM segmento (nem em NOVO) até que isso
            // aconteça pelo menos uma vez. Use o filtro TODOS pra alcançar
            // esses clientes.
            destinatarios = clienteRepository
                    .findByEstabelecimentoIdAndEstagioAtual(estabelecimento.getId(), filtroEstagio)
                    .stream()
                    .filter(c -> !c.isBloqueado())
                    .toList();
        }

        // ── Vínculo com profissional(is) ────────────────────────────────────
        boolean todosProfissionais = profissionalIds == null || profissionalIds.isEmpty();
        List<Profissional> profissionaisVinculados = List.of();
        if (!todosProfissionais) {
            profissionaisVinculados = profissionalRepository
                    .findByEstabelecimentoIdAndIdIn(estabelecimento.getId(), profissionalIds);
            if (profissionaisVinculados.isEmpty()) {
                throw new CampanhaEnvioException("Nenhum dos profissionais informados foi encontrado neste estabelecimento.");
            }
            List<Long> idsEncontrados = profissionaisVinculados.stream().map(Profissional::getId).toList();
            Set<Long> clienteIdsDoProfissional = Set.copyOf(
                    agendamentoRepository.findClienteIdsDistinctByEstabelecimentoIdAndProfissionalIdIn(
                            estabelecimento.getId(), idsEncontrados));
            destinatarios = destinatarios.stream()
                    .filter(c -> clienteIdsDoProfissional.contains(c.getId()))
                    .toList();
        }

        if (destinatarios.isEmpty()) {
            String detalhe = (filtroEstagio == EstagioCliente.TODOS)
                    ? "Nenhum cliente cadastrado (ou todos estão bloqueados)."
                    : "Nenhum cliente encontrado para o segmento '" + filtroEstagio + "'. Isso pode acontecer porque "
                    + "o estágio do cliente só é calculado quando um atendimento dele é concluído — clientes "
                    + "cadastrados manualmente e sem histórico ainda não têm estágio definido. Use o filtro "
                    + "'TODOS' pra alcançar a base inteira, independente do estágio.";
            if (!todosProfissionais) {
                detalhe = "Nenhum cliente do segmento '" + filtroEstagio
                        + "' tem histórico de agendamento com o(s) profissional(is) selecionado(s).";
            }
            throw new CampanhaEnvioException(detalhe + " Nenhum rascunho foi criado.");
        }

        // ── Cupom opcional: reaproveita um existente OU cria um novo ────────
        CupomEstabelecimento cupomGerado;
        if (cupomExistenteId != null) {
            try {
                cupomGerado = cupomEstabelecimentoService.buscarDoEstabelecimento(estabelecimento, cupomExistenteId);
            } catch (RuntimeException e) {
                throw new CampanhaEnvioException("Cupom selecionado não foi encontrado neste estabelecimento.");
            }
        } else if (cupomConfig != null) {
            cupomGerado = gerarCupomDaCampanha(estabelecimento, cupomConfig);
        } else {
            cupomGerado = null;
        }

        String templateEstabelecimento = estabelecimento.getTemplateMensagemCampanha();
        boolean usaTemplate = templateEstabelecimento != null && !templateEstabelecimento.isBlank();

        CampanhaEnvio campanha = new CampanhaEnvio();
        campanha.setEstabelecimento(estabelecimento);
        campanha.setFiltroEstagio(filtroEstagio);
        campanha.setMensagem(mensagem);
        campanha.setUsouTemplate(usaTemplate);
        campanha.setOrigem("MANUAL");
        campanha.setStatus("RASCUNHO");
        campanha.setTotalDestinatarios(destinatarios.size());
        campanha.setTodosProfissionais(todosProfissionais);
        campanha.setProfissionais(profissionaisVinculados);
        campanha.setCupomEstabelecimento(cupomGerado);
        campanha = campanhaRepository.save(campanha);

        // Trecho do cupom anexado uma vez só (o link é o mesmo pra todo
        // mundo — cupom compartilhado da campanha), reaproveitado em cada
        // mensagemFinal.
        String trechoCupom = cupomGerado != null ? montarTrechoCupom(estabelecimento, cupomGerado) : null;

        for (int i = 0; i < destinatarios.size(); i++) {
            Cliente cliente = destinatarios.get(i);
            int indiceLote = i / TAMANHO_LOTE_DISTRIBUICAO;
            String saudacao = VARIACOES_SAUDACAO.get(indiceLote % VARIACOES_SAUDACAO.size());
            String mensagemFinal = montarMensagemFinal(estabelecimento, cliente, mensagem, saudacao, usaTemplate);
            if (trechoCupom != null) {
                mensagemFinal = mensagemFinal + trechoCupom;
            }

            CampanhaEnvioItem item = new CampanhaEnvioItem(campanha, cliente, cliente.getTelefone());
            item.setMensagemFinal(mensagemFinal);
            item.setStatus("RASCUNHO");
            itemRepository.save(item);
        }

        return campanha;
    }

    // Parâmetros de criação do cupom, desacoplado do DTO da camada de
    // aplicação de propósito (mesmo padrão dos demais métodos deste
    // service, que recebem tipos primitivos/próprios do domínio).
    public record CupomConfig(String codigo, String tipoDesconto, BigDecimal valorDesconto,
                              Integer diasValidade, Integer limiteUsos) {}

    private CupomEstabelecimento gerarCupomDaCampanha(Estabelecimento estabelecimento, CupomConfig cupomConfig) {
        String codigo = (cupomConfig.codigo() != null && !cupomConfig.codigo().isBlank())
                ? cupomConfig.codigo().trim()
                // Código automático quando o dono não escolhe um: prefixo
                // fixo + timestamp, mesmo padrão de unicidade já usado em
                // CampanhaAutomaticaService.gerarCupomReativacaoOuNull.
                : "PROMO" + (System.currentTimeMillis() % 1000000);

        // diasValidade nulo = sem expiração (mesma semântica de
        // CupomEstabelecimentoService.criar, que já aceita validadeData
        // nula). Quando informado, validadeData = hoje + diasValidade, e o
        // próprio service valida o teto de 7 dias — não duplicamos essa
        // regra aqui.
        LocalDate validadeData = cupomConfig.diasValidade() != null
                ? LocalDate.now().plusDays(cupomConfig.diasValidade())
                : null;

        return cupomEstabelecimentoService.criar(
                estabelecimento,
                codigo,
                cupomConfig.tipoDesconto(),
                cupomConfig.valorDesconto(),
                validadeData,
                cupomConfig.limiteUsos());
    }

    // Mesmo formato de link já usado em CampanhaAutomaticaService /
    // AgendamentoPublicoController#validarCupom: baseUrl + /app/{slug}/cupom/{codigo}.
    // Formato CORRIGIDO: a página pública de agendamento é /app/{slug} e lê
    // o cupom da query string (?cupom=CODIGO) pra aplicar o desconto antes
    // da confirmação — não uma rota própria por path. O formato antigo
    // (/app/{slug}/cupom/{codigo}) só bate com o endpoint de API
    // AgendamentoPublicoController#validarCupom (JSON puro), não com
    // nenhuma rota do front, e por isso caía direto na landing page.
    private String montarTrechoCupom(Estabelecimento estabelecimento, CupomEstabelecimento cupom) {
        String link = baseUrl + "/app/" + estabelecimento.getSlug() + "?cupom=" + cupom.getCodigo();
        return "\n\n🎁 Use o cupom *" + cupom.getCodigo() + "*: " + link;
    }

    private String montarMensagemFinal(Estabelecimento estabelecimento, Cliente cliente,
                                       String textoLivre, String saudacao, boolean usaTemplate) {
        String primeiroNome = primeiroNome(cliente.getNome());
        String corpo = saudacao + (primeiroNome != null ? ", " + primeiroNome + "!" : "!") + "\n\n" + textoLivre;

        if (!usaTemplate) {
            return corpo;
        }

        String template = estabelecimento.getTemplateMensagemCampanha();
        String resultado = template
                .replace("{{mensagem}}", corpo)
                .replace("{{nome_cliente}}", primeiroNome != null ? primeiroNome : "")
                .replace("{{nome_estabelecimento}}", estabelecimento.getNome() != null ? estabelecimento.getNome() : "");

        if (!template.contains("{{mensagem}}")) {
            resultado = resultado + "\n\n" + corpo;
        }
        return resultado;
    }

    private String primeiroNome(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) return null;
        String trim = nomeCompleto.trim();
        int espaco = trim.indexOf(' ');
        return espaco > 0 ? trim.substring(0, espaco) : trim;
    }

    // ── Confirmação (dispara de verdade, libera pro job) ────────────────────

    @Transactional
    public CampanhaEnvio confirmar(Estabelecimento estabelecimento, Long campanhaId) {
        CampanhaEnvio campanha = buscarDoEstabelecimento(estabelecimento, campanhaId);

        if (!"RASCUNHO".equals(campanha.getStatus())) {
            throw new CampanhaEnvioException(
                    "Esta campanha não pode mais ser confirmada (status atual: " + campanha.getStatus() + ").");
        }
        if (estabelecimento.getEvolutionInstanceName() == null
                || estabelecimento.getEvolutionInstanceName().isBlank()) {
            throw new CampanhaEnvioException("WhatsApp não está conectado para este estabelecimento.");
        }

        // Anti-banimento: intervalo mínimo configurável entre campanhas.
        campanhaRepository.buscarUltimaConfirmada(estabelecimento.getId()).ifPresent(ultima -> {
            long diasDesdeUltima = ChronoUnit.DAYS.between(ultima.getCriadoEm(), LocalDateTime.now());
            int minimoExigido = Math.max(1, estabelecimento.getIntervaloMinimoCampanhaDias());
            if (diasDesdeUltima < minimoExigido) {
                long faltam = minimoExigido - diasDesdeUltima;
                throw new CampanhaEnvioException(
                        "Para reduzir o risco de bloqueio do WhatsApp, é preciso esperar pelo menos "
                                + minimoExigido + " dia(s) entre campanhas. Faltam " + faltam
                                + " dia(s) para poder confirmar uma nova campanha.");
            }
        });

        List<CampanhaEnvioItem> itensRascunho =
                itemRepository.findByCampanhaIdAndStatusOrderByCriadoEmAsc(campanha.getId(), "RASCUNHO");

        if (itensRascunho.isEmpty()) {
            throw new CampanhaEnvioException("Esta campanha não tem destinatários pendentes de confirmação.");
        }

        // Espalha o horário de envio em lotes (anti-banimento): dentro de
        // cada lote os itens ficam PENDENTE desde já; o campo agendadoPara é
        // o que faz o job só pegá-los no horário correto.
        LocalDateTime agora = LocalDateTime.now();
        for (int i = 0; i < itensRascunho.size(); i++) {
            int indiceLote = i / TAMANHO_LOTE_DISTRIBUICAO;
            CampanhaEnvioItem item = itensRascunho.get(i);
            item.setAgendadoPara(agora.plusMinutes(indiceLote * MINUTOS_ENTRE_LOTES));
            item.setStatus("PENDENTE");
            itemRepository.save(item);
        }

        campanha.setStatus("CRIADA");
        return campanhaRepository.save(campanha);
    }

    // ── Cancelamento de um rascunho ainda não confirmado ────────────────────

    @Transactional
    public CampanhaEnvio cancelar(Estabelecimento estabelecimento, Long campanhaId) {
        CampanhaEnvio campanha = buscarDoEstabelecimento(estabelecimento, campanhaId);

        if (!"RASCUNHO".equals(campanha.getStatus())) {
            throw new CampanhaEnvioException(
                    "Só é possível cancelar uma campanha que ainda está em rascunho (status atual: "
                            + campanha.getStatus() + ").");
        }

        itemRepository.cancelarItensDaCampanha(campanha.getId());
        campanha.setStatus("CANCELADA");
        campanha.setConcluidoEm(LocalDateTime.now());

        // Se essa campanha tinha um cupom gerado só pra ela, desativa junto
        // — não faz sentido o cupom continuar válido pra uma campanha que
        // nunca vai sair do papel.
        if (campanha.getCupomEstabelecimento() != null) {
            cupomEstabelecimentoService.desativar(estabelecimento, campanha.getCupomEstabelecimento().getId());
        }

        return campanhaRepository.save(campanha);
    }

    // ── Leitura ──────────────────────────────────────────────────────────────

    public List<CampanhaEnvio> listar(Estabelecimento estabelecimento) {
        return campanhaRepository.findByEstabelecimentoIdOrderByCriadoEmDesc(estabelecimento.getId());
    }

    public CampanhaEnvio buscarDoEstabelecimento(Estabelecimento estabelecimento, Long campanhaId) {
        return campanhaRepository.findByIdAndEstabelecimentoId(campanhaId, estabelecimento.getId())
                .orElseThrow(() -> new CampanhaEnvioException("Campanha não encontrada."));
    }
}