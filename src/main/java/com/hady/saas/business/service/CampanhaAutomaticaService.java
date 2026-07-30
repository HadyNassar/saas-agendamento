package com.hady.saas.business.service;

import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.cliente.ClienteEstagioHistorico;
import com.hady.saas.domain.cupom.CupomEstabelecimento;
import com.hady.saas.domain.enums.EstagioCliente;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.whatsapp.CampanhaEnvio;
import com.hady.saas.domain.whatsapp.CampanhaEnvioItem;
import com.hady.saas.infrastructure.repository.CampanhaEnvioItemRepository;
import com.hady.saas.infrastructure.repository.CampanhaEnvioRepository;
import com.hady.saas.infrastructure.repository.ClienteEstagioHistoricoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Gatilho automático de mensagem por mudança de estágio — cobre o
 * "reconhecimento/reativação" original (itens 9/10: VIP e INATIVO), o funil
 * completo (item 11: TODO estágio dispara mensagem própria) e, quando o
 * dono do salão ativar explicitamente, o cupom automático de reativação
 * (item 8: um cupom novo é gerado e anexado à mensagem sempre que um
 * cliente vira INATIVO). Diferente da CampanhaEnvioService (seção 5-B), que
 * é o dono do salão disparando na mão, aqui é o próprio sistema que decide
 * disparar, sem nenhuma ação humana.
 *
 * Reaproveita 100% a mesma fila técnica (CampanhaEnvio / CampanhaEnvioItem /
 * CampanhaEnvioProcessorJob) — este service só é mais um CRIADOR de
 * CampanhaEnvio, assim como CampanhaEnvioService já é.
 *
 * Gatilho: sempre que ClienteSegmentacaoService.recalcularEPersistir() grava
 * uma transição de estágio em cliente_estagio_historico, essa linha nasce com
 * processadoCampanhaAutomatica=false. Este service roda uma vez por dia,
 * busca TODAS as transições ainda não avaliadas (agora os 4 estágios, não só
 * VIP/INATIVO), cria UMA CampanhaEnvio de um único destinatário para cada
 * cliente afetado, e marca a linha de histórico como processada.
 *
 * Diferente da campanha manual (RASCUNHO -> confirmar), aqui a CampanhaEnvio
 * já nasce em status CRIADA — é uma regra do sistema, não faz sentido pedir
 * confirmação do dono do salão pra cada cliente que muda de estágio.
 *
 * IMPORTANTE — decisões confirmadas com o dono do produto (27/07/2026):
 *   - Texto das mensagens dos 4 estágios (mensagemPadrao) e a frase do cupom
 *     de reativação foram revisados para tom caloroso/com emoji, o mesmo
 *     padrão usado em WhatsAppService.enviarSolicitacaoAvaliacao e
 *     enviarAgradecimentoNotaAlta. Aprovado como padrão definitivo — não são
 *     mais placeholder. O cupom (item 8) só entra na mensagem de INATIVO, e
 *     só se o dono tiver ativado a função — ver regras específicas do cupom
 *     mais abaixo.
 *   - A primeíssima classificação de um cliente (estagioAnterior == null,
 *     sempre NOVO) também dispara mensagem pelo mesmo mecanismo — na prática
 *     funciona como um "obrigado pela primeira visita". Se isso for
 *     indesejado (ex: por já existir uma mensagem de confirmação de
 *     agendamento separada), dá pra excluir esse caso específico depois; não
 *     exclui por padrão porque não foi pedido.
 *   - Horário do disparo diário (9h) é arbitrário, ajustar se fizer sentido
 *     rodar em outro horário.
 *   - Clientes bloqueados (Cliente.isBloqueado()) são sempre ignorados, sem
 *     exceção — mesma regra já usada em CampanhaEnvioService.
 *   - Estabelecimento sem WhatsApp conectado (evolutionInstanceName vazio)
 *     ou no plano STANDARD tem a transição marcada como processada mas
 *     nenhuma campanha é criada — não fica reprocessando pra sempre, mas
 *     também não gera erro.
 *
 * Regras do cupom automático (item 8) confirmadas com o dono do produto:
 *   - Só dispara SE o dono ligar explicitamente (CupomAutomaticoController)
 *     — nunca gera cupom pra ninguém que não tenha ativado a função.
 *   - Desconto (percentual ou valor fixo) é configurável pelo dono.
 *   - Cupom vale 7 dias a partir da geração.
 *   - Pode gerar de novo pro mesmo cliente se ele ficar INATIVO outra vez
 *     no futuro (sem trava de "um cupom por cliente pra sempre").
 *   - O cliente recebe o link já com o cupom aplicado, não um código pra
 *     digitar.
 */
@Service
public class CampanhaAutomaticaService {

    private static final Logger log = LoggerFactory.getLogger(CampanhaAutomaticaService.class);

    // Item 11 (funil completo): os 4 estágios disparam mensagem, não só
    // VIP/INATIVO como na primeira versão (itens 9/10).
    private static final List<EstagioCliente> ESTAGIOS_COM_GATILHO = List.of(EstagioCliente.values());

    private final ClienteEstagioHistoricoRepository historicoRepository;
    private final CampanhaEnvioRepository campanhaRepository;
    private final CampanhaEnvioItemRepository itemRepository;
    private final CupomEstabelecimentoService cupomEstabelecimentoService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public CampanhaAutomaticaService(ClienteEstagioHistoricoRepository historicoRepository,
                                     CampanhaEnvioRepository campanhaRepository,
                                     CampanhaEnvioItemRepository itemRepository,
                                     CupomEstabelecimentoService cupomEstabelecimentoService) {
        this.historicoRepository = historicoRepository;
        this.campanhaRepository = campanhaRepository;
        this.itemRepository = itemRepository;
        this.cupomEstabelecimentoService = cupomEstabelecimentoService;
    }

    // Roda uma vez por dia às 9h. Não precisa de granularidade maior — o
    // gatilho não é urgente (diferente de um lembrete de agendamento) e
    // rodar 1x/dia evita qualquer chance de disparo duplicado por corrida
    // entre execuções concorrentes.
    @Scheduled(cron = "0 0 9 * * *")
    public void executarGatilhoDiario() {
        List<ClienteEstagioHistorico> pendentes =
                historicoRepository.findByProcessadoCampanhaAutomaticaFalseAndEstagioNovoIn(ESTAGIOS_COM_GATILHO);

        if (pendentes.isEmpty()) return;

        log.info("[CampanhaAutomatica] {} transição(ões) de estágio pendente(s) de avaliação", pendentes.size());

        for (ClienteEstagioHistorico transicao : pendentes) {
            processarTransicao(transicao);
        }
    }

    @Transactional
    protected void processarTransicao(ClienteEstagioHistorico transicao) {
        try {
            Cliente cliente = transicao.getCliente();
            Estabelecimento estabelecimento = transicao.getEstabelecimento();
            EstagioCliente estagio = transicao.getEstagioNovo();

            boolean podeDisparar = estabelecimento.getPlanoEfetivo() != Plano.STANDARD
                    && estabelecimento.getEvolutionInstanceName() != null
                    && !estabelecimento.getEvolutionInstanceName().isBlank()
                    && !cliente.isBloqueado()
                    && cliente.getTelefone() != null
                    && !cliente.getTelefone().isBlank();

            if (podeDisparar) {
                String mensagem = mensagemPadrao(estagio, cliente.getNome(), estabelecimento.getNome());

                // Item 8: só pro estágio INATIVO, e só se o dono tiver
                // ligado a função explicitamente (regra confirmada).
                if (estagio == EstagioCliente.INATIVO && estabelecimento.isCupomReativacaoAutomaticoAtivo()) {
                    String linkComCupom = gerarCupomReativacaoOuNull(estabelecimento, cliente);
                    if (linkComCupom != null) {
                        mensagem = mensagem + "\n\n🎁 Preparamos um cupom especial pra você voltar: " + linkComCupom;
                    }
                    // Se a geração do cupom falhar por qualquer motivo, a
                    // mensagem de texto normal ainda assim é enviada — não
                    // faz sentido deixar de mandar o "sentimos sua falta"
                    // só porque o cupom deu erro.
                }

                CampanhaEnvio campanha = new CampanhaEnvio();
                campanha.setEstabelecimento(estabelecimento);
                campanha.setFiltroEstagio(estagio);
                campanha.setMensagem(mensagem);
                campanha.setOrigem("AUTOMATICA");
                campanha.setStatus("CRIADA");
                campanha.setTotalDestinatarios(1);
                campanha = campanhaRepository.save(campanha);

                CampanhaEnvioItem item = new CampanhaEnvioItem(campanha, cliente, cliente.getTelefone());
                item.setStatus("PENDENTE");
                itemRepository.save(item);

                log.info("[CampanhaAutomatica] Campanha id={} criada para cliente id={} (estágio {})",
                        campanha.getId(), cliente.getId(), estagio);
            } else {
                log.debug("[CampanhaAutomatica] Transição id={} (cliente id={}, estágio {}) não elegível, pulando.",
                        transicao.getId(), cliente.getId(), estagio);
            }

            transicao.setProcessadoCampanhaAutomatica(true);
            historicoRepository.save(transicao);
        } catch (Exception e) {
            // Uma falha isolada não pode travar o processamento das demais
            // transições do lote do dia — loga e segue. A transição fica
            // como NÃO processada, então será reavaliada no próximo dia.
            log.error("[CampanhaAutomatica] Falha ao processar transição id={}: {}",
                    transicao.getId(), e.getMessage(), e);
        }
    }

    // Item 8 (cupom automático de reativação). Só chamado quando o dono já
    // ativou a função e configurou um desconto (CupomAutomaticoController).
    // Gera um cupom NOVO a cada vez que o cliente vira INATIVO — combinado
    // explicitamente que não há trava de "um cupom por cliente pra sempre";
    // se ele reativar e ficar INATIVO de novo mais pra frente, ganha outro.
    // Código inclui o id do cliente + timestamp pra garantir unicidade sem
    // precisar checar colisão manualmente.
    private String gerarCupomReativacaoOuNull(Estabelecimento estabelecimento, Cliente cliente) {
        try {
            String codigo = "VOLTA" + cliente.getId() + "-" + System.currentTimeMillis() % 100000;
            CupomEstabelecimento cupom = cupomEstabelecimentoService.criar(
                    estabelecimento,
                    codigo,
                    estabelecimento.getCupomReativacaoTipoDesconto(),
                    estabelecimento.getCupomReativacaoValorDesconto(),
                    LocalDate.now().plusDays(7), // regra confirmada: 7 dias de validade
                    1 // um único uso — é um cupom pessoal pra este cliente, não geral
            );

            // Mesmo padrão de link já usado em WhatsAppService.enviarLembreteRetorno
            // (baseUrl + /app/{slug}), com o cupom aplicado via path — mesmo
            // formato do endpoint público de validação já existente
            // (AgendamentoPublicoController#validarCupom).
            return baseUrl + "/app/" + estabelecimento.getSlug() + "/cupom/" + cupom.getCodigo();
        } catch (Exception e) {
            log.error("[CampanhaAutomatica] Falha ao gerar cupom de reativação pro cliente id={}: {}",
                    cliente.getId(), e.getMessage(), e);
            return null;
        }
    }

    // Placeholder — ver aviso na doc da classe. Não validado com o dono do
    // produto ainda. Item 11: agora cobre os 4 estágios do funil.
    // Tom revisado (27/07/2026) — mesmo estilo caloroso/emoji das mensagens de
    // avaliação e agradecimento (WhatsAppService.enviarSolicitacaoAvaliacao /
    // enviarAgradecimentoNotaAlta), aprovado como padrão definitivo pelo dono
    // do produto. Não é mais placeholder.
    private String mensagemPadrao(EstagioCliente estagio, String nomeCliente, String nomeEstabelecimento) {
        String primeiroNome = (nomeCliente == null || nomeCliente.isBlank())
                ? "" : nomeCliente.trim().split(" ")[0];

        return switch (estagio) {
            case NOVO -> "Oi, " + primeiroNome + "! 😊\n\n"
                    + "Muito obrigado por escolher a *" + nomeEstabelecimento + "* pra sua primeira visita! "
                    + "Foi um prazer ter você por aqui.\n\n"
                    + "Esperamos te ver de novo em breve! ✨";

            case RECORRENTE -> "Oi, " + primeiroNome + "! 🙌\n\n"
                    + "Que bom ter você sempre por perto na *" + nomeEstabelecimento + "*! "
                    + "Sua presença faz toda diferença pra gente.\n\n"
                    + "Precisando marcar um novo horário, é só chamar por aqui! 💬";

            case VIP -> "Oi, " + primeiroNome + "! 🌟\n\n"
                    + "Você já é cliente frequente aqui na *" + nomeEstabelecimento + "* e isso é muito especial pra gente. "
                    + "Muito obrigado pela confiança!\n\n"
                    + "Qualquer coisa que precisar, estamos por aqui! 💛";

            case INATIVO -> "Oi, " + primeiroNome + "! 🥺\n\n"
                    + "Sentimos sua falta na *" + nomeEstabelecimento + "*! Faz um tempinho que você não aparece "
                    + "e queríamos muito te ver de novo.\n\n"
                    + "Que tal agendar um novo horário? É só responder essa mensagem! 📅";

            // TODOS é um pseudo-segmento usado apenas como filtro de campanha
            // manual (CampanhaEnvioService) — nunca é o estagioAtual real de
            // um Cliente, então este branch não deveria ser alcançado na
            // prática. Cobrimos aqui só para satisfazer a exaustividade do
            // switch, que passou a ser exigida pelo compilador após TODOS
            // ser adicionado ao enum EstagioCliente.
            case TODOS -> "Oi, " + primeiroNome + "! 😊\n\n"
                    + "Temos novidades na *" + nomeEstabelecimento + "*! Que tal agendar um horário com a gente? 📅";
        };
    }
}