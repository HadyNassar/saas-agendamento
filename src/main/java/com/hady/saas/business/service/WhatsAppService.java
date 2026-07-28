package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.whatsapp.WhatsAppMensagem;
import com.hady.saas.infrastructure.client.EvolutionApiClient;
import com.hady.saas.infrastructure.repository.WhatsAppMensagemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final EvolutionApiClient evolutionApiClient;
    private final WhatsAppMensagemRepository mensagemRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    public WhatsAppService(EvolutionApiClient evolutionApiClient,
                           WhatsAppMensagemRepository mensagemRepository) {
        this.evolutionApiClient = evolutionApiClient;
        this.mensagemRepository = mensagemRepository;
    }

    // ─── Verificação ──────────────────────────────────────────────────────────

    private boolean podeEnviar(Agendamento agendamento) {
        var est = agendamento.getEstabelecimento();
        return est.isWhatsappConectado()
                && est.getEvolutionInstanceName() != null
                && agendamento.getCliente().getTelefone() != null;
    }

    // ─── Envio com log no banco ───────────────────────────────────────────────

    private void enviarComLog(Agendamento agendamento, String mensagem, String tipo) {
        WhatsAppMensagem logMsg = new WhatsAppMensagem();
        logMsg.setEstabelecimento(agendamento.getEstabelecimento());
        logMsg.setAgendamento(agendamento);
        logMsg.setCliente(agendamento.getCliente());
        logMsg.setTelefoneDestino(agendamento.getCliente().getTelefone());
        logMsg.setMensagem(mensagem);
        logMsg.setTipo(tipo);

        try {
            evolutionApiClient.enviarMensagem(
                    agendamento.getEstabelecimento().getEvolutionInstanceName(),
                    agendamento.getCliente().getTelefone(),
                    mensagem
            );
            logMsg.setStatus("ENVIADO");
            logMsg.setEnviadoEm(LocalDateTime.now());
        } catch (Exception e) {
            logMsg.setStatus("ERRO");
            logMsg.setErroDetalhe(e.getMessage());
            log.error("[WhatsApp] Erro ao enviar ({}): {}", tipo, e.getMessage());
        }

        mensagemRepository.save(logMsg);
    }

    // ─── Confirmação ──────────────────────────────────────────────────────────

    public void enviarConfirmacao(Agendamento agendamento) {
        if (!podeEnviar(agendamento)) return;

        String mensagem = String.format(
                "Olá, %s! 👋\n\n" +
                        "✅ Seu agendamento foi confirmado!\n\n" +
                        "📋 *Serviço:* %s\n" +
                        "💇 *Profissional:* %s\n" +
                        "📅 *Data:* %s\n" +
                        "🕐 *Horário:* %s\n\n" +
                        "📍 *%s*\n\n" +
                        "Te esperamos! 😊",
                agendamento.getCliente().getNome().split(" ")[0],
                agendamento.getServico().getNome(),
                agendamento.getProfissional().getNome(),
                agendamento.getDataHoraInicio().format(FMT_DATA),
                agendamento.getDataHoraInicio().format(FMT_HORA),
                agendamento.getEstabelecimento().getNome()
        );

        enviarComLog(agendamento, mensagem, "CONFIRMACAO");
    }

    // Texto do prompt interativo do item 2 (camada ELITE). Só entra na mensagem
    // quando LembreteConfirmacaoService.elegivel() disser que sim — no plano
    // STANDARD o lembrete continua idêntico a como sempre foi.
    private static final String PROMPT_CONFIRMACAO =
            "\n\n👉 Responda *1* para confirmar presença ou *2* para cancelar.";

    // ─── Lembrete 24h ────────────────────────────────────────────────────────

    // incluirPromptConfirmacao: item 2 — quando true, adiciona "responda 1/2" ao
    // final da mensagem. Decidido fora daqui (NotificacaoService + LembreteConfirmacaoService),
    // pra manter este serviço só preocupado com formatação/envio de texto.
    public void enviarLembrete24h(Agendamento agendamento, boolean incluirPromptConfirmacao) {
        if (!podeEnviar(agendamento)) return;

        String mensagem = String.format(
                "Olá, %s! 👋\n\n" +
                        "🔔 Lembrando que *amanhã* você tem um agendamento!\n\n" +
                        "📋 *Serviço:* %s\n" +
                        "💇 *Profissional:* %s\n" +
                        "📅 *Data:* %s\n" +
                        "🕐 *Horário:* %s\n\n" +
                        "📍 *%s*\n\n" +
                        "Qualquer dúvida, entre em contato! 😊",
                agendamento.getCliente().getNome().split(" ")[0],
                agendamento.getServico().getNome(),
                agendamento.getProfissional().getNome(),
                agendamento.getDataHoraInicio().format(FMT_DATA),
                agendamento.getDataHoraInicio().format(FMT_HORA),
                agendamento.getEstabelecimento().getNome()
        );
        if (incluirPromptConfirmacao) mensagem += PROMPT_CONFIRMACAO;

        enviarComLog(agendamento, mensagem, "LEMBRETE_24H");
    }

    // ─── Lembrete 2h ─────────────────────────────────────────────────────────

    public void enviarLembrete2h(Agendamento agendamento, boolean incluirPromptConfirmacao) {
        if (!podeEnviar(agendamento)) return;

        String mensagem = String.format(
                "Olá, %s! 👋\n\n" +
                        "⏰ Seu agendamento é *daqui a 2 horas*!\n\n" +
                        "📋 *Serviço:* %s\n" +
                        "💇 *Profissional:* %s\n" +
                        "🕐 *Horário:* %s\n\n" +
                        "📍 *%s*\n\n" +
                        "Te esperamos em breve! 😊",
                agendamento.getCliente().getNome().split(" ")[0],
                agendamento.getServico().getNome(),
                agendamento.getProfissional().getNome(),
                agendamento.getDataHoraInicio().format(FMT_HORA),
                agendamento.getEstabelecimento().getNome()
        );
        if (incluirPromptConfirmacao) mensagem += PROMPT_CONFIRMACAO;

        enviarComLog(agendamento, mensagem, "LEMBRETE_2H");
    }

    // ─── Lembrete manual ─────────────────────────────────────────────────────

    public void enviarLembrete(Agendamento agendamento) {
        if (!podeEnviar(agendamento)) return;

        String mensagem = String.format(
                "Olá, %s! 👋\n\n" +
                        "🔔 *Lembrete de agendamento!*\n\n" +
                        "📋 *Serviço:* %s\n" +
                        "💇 *Profissional:* %s\n" +
                        "📅 *Data:* %s\n" +
                        "🕐 *Horário:* %s\n\n" +
                        "📍 *%s*\n\n" +
                        "Te esperamos! 😊",
                agendamento.getCliente().getNome().split(" ")[0],
                agendamento.getServico().getNome(),
                agendamento.getProfissional().getNome(),
                agendamento.getDataHoraInicio().format(FMT_DATA),
                agendamento.getDataHoraInicio().format(FMT_HORA),
                agendamento.getEstabelecimento().getNome()
        );

        enviarComLog(agendamento, mensagem, "LEMBRETE");
    }

    // ─── Cancelamento ─────────────────────────────────────────────────────────

    public void enviarCancelamento(Agendamento agendamento) {
        if (!podeEnviar(agendamento)) return;

        String mensagem = String.format(
                "Olá, %s! 👋\n\n" +
                        "❌ Seu agendamento foi *cancelado*.\n\n" +
                        "📋 *Serviço:* %s\n" +
                        "📅 *Data:* %s às %s\n\n" +
                        "Para remarcar, acesse nosso link de agendamento. 😊\n\n" +
                        "📍 *%s*",
                agendamento.getCliente().getNome().split(" ")[0],
                agendamento.getServico().getNome(),
                agendamento.getDataHoraInicio().format(FMT_DATA),
                agendamento.getDataHoraInicio().format(FMT_HORA),
                agendamento.getEstabelecimento().getNome()
        );

        enviarComLog(agendamento, mensagem, "CANCELAMENTO");
    }

    // ─── Falta ────────────────────────────────────────────────────────────────

    public void enviarFalta(Agendamento agendamento) {
        if (!podeEnviar(agendamento)) return;

        String mensagem = String.format(
                "Olá, %s! 👋\n\n" +
                        "⚠️ Notamos que você *não compareceu* ao seu agendamento de hoje.\n\n" +
                        "📋 *Serviço:* %s\n" +
                        "🕐 *Horário:* %s\n\n" +
                        "Quando quiser remarcar, é só acessar nosso link. 😊\n\n" +
                        "📍 *%s*",
                agendamento.getCliente().getNome().split(" ")[0],
                agendamento.getServico().getNome(),
                agendamento.getDataHoraInicio().format(FMT_HORA),
                agendamento.getEstabelecimento().getNome()
        );

        enviarComLog(agendamento, mensagem, "FALTA");
    }

    // ─── AVALIAÇÃO PÓS-ATENDIMENTO ────────────────────────────────────────────
    // Enviado ~1h após o atendimento ser marcado como CONCLUÍDO.
    // O link leva o cliente para uma página pública que registra a nota.

    public void enviarSolicitacaoAvaliacao(Agendamento agendamento) {
        if (!podeEnviar(agendamento)) return;

        String linkAvaliacao = baseUrl + "/app/"
                + agendamento.getEstabelecimento().getSlug()
                + "/avaliacao/" + agendamento.getId();

        String mensagem = String.format(
                "Olá, %s! 😊\n\n" +
                        "Que bom ter você em *%s* hoje!\n\n" +
                        "Seu atendimento foi concluído. O que achou?\n\n" +
                        "⭐ Avalie seu atendimento em apenas 1 clique:\n" +
                        "%s\n\n" +
                        "Sua opinião é muito importante para nós! 🙏",
                agendamento.getCliente().getNome().split(" ")[0],
                agendamento.getEstabelecimento().getNome(),
                linkAvaliacao
        );

        enviarComLog(agendamento, mensagem, "AVALIACAO");
    }

    // ─── AGRADECIMENTO POR AVALIAÇÃO COM NOTA ALTA (item 16) ─────────────────
    // Enviado quando o cliente registra nota 5 na avaliação pós-atendimento.
    // Reforço positivo simples, sem link de review externo (não existe página
    // de review público fora do app hoje).

    public void enviarAgradecimentoNotaAlta(Agendamento agendamento) {
        enviarAgradecimentoNotaAlta(agendamento, null);
    }

    // Sobrecarga com link de indicação opcional (item 17). Quando não nulo,
    // o link é anexado à mesma mensagem de agradecimento — evita mandar uma
    // segunda mensagem separada só pra convidar a indicar alguém.
    public void enviarAgradecimentoNotaAlta(Agendamento agendamento, String linkIndicacaoOuNull) {
        if (!podeEnviar(agendamento)) return;

        String mensagem = String.format(
                "Que alegria, %s! 🥰\n\n" +
                        "Muito obrigado pela sua avaliação em *%s*! Ficamos muito felizes " +
                        "em saber que você saiu satisfeito(a) do atendimento.\n\n" +
                        "Esperamos te ver novamente em breve! ✨",
                agendamento.getCliente().getNome().split(" ")[0],
                agendamento.getEstabelecimento().getNome()
        );

        if (linkIndicacaoOuNull != null && !linkIndicacaoOuNull.isBlank()) {
            mensagem += "\n\n👯 Ah, e já que você curtiu tanto: que tal indicar uma amiga? "
                    + "Vocês duas ganham um cupom especial quando ela fizer o primeiro atendimento aqui:\n"
                    + linkIndicacaoOuNull;
        }

        enviarComLog(agendamento, mensagem, "AGRADECIMENTO_AVALIACAO");
    }

    // ─── LEMBRETE DE RETORNO AUTOMÁTICO ──────────────────────────────────────
    // Enviado ~30 dias após o último atendimento concluído do cliente.
    // Disponível apenas nos planos ELITE e PLATINUM.

    public void enviarLembreteRetorno(Agendamento ultimoAgendamento) {
        if (!podeEnviar(ultimoAgendamento)) return;

        String linkAgendamento = baseUrl + "/app/"
                + ultimoAgendamento.getEstabelecimento().getSlug();

        String mensagem = String.format(
                "Olá, %s! 👋\n\n" +
                        "Sentimos sua falta em *%s*! 💇\n\n" +
                        "Faz um tempinho desde o seu último atendimento de *%s*.\n\n" +
                        "Que tal agendar uma visita?\n" +
                        "👉 %s\n\n" +
                        "Estamos te esperando! 😊",
                ultimoAgendamento.getCliente().getNome().split(" ")[0],
                ultimoAgendamento.getEstabelecimento().getNome(),
                ultimoAgendamento.getServico().getNome(),
                linkAgendamento
        );

        enviarComLog(ultimoAgendamento, mensagem, "LEMBRETE_RETORNO");
    }

    // ─── ITEM 15 (PLATINUM/ELITE) — PAGAMENTO VIA WHATSAPP ───────────────────
    // Mensagem transacional (não é robô/marketing) — mesmo padrão de
    // confirmação/lembretes/cancelamento: disponível em TODOS os planos que
    // tenham pagamento online habilitado (Estabelecimento.aceitaPagamento +
    // Mercado Pago conectado), independentemente de ELITE/PLATINUM/STANDARD.
    // O link leva sempre para a página web de pagamento já existente
    // (/app/{slug}/pagamento/{agendamentoId} — confirmado com o dono do
    // produto que essa página já existe no frontend). O cliente escolhe PIX
    // ou cartão lá; não geramos PIX automaticamente pelo backend aqui
    // (decisão de produto: manter simples, sem gerar cobrança sem o cliente
    // estar na tela escolhendo o método).
    //
    // Duas mensagens diferentes pra dois cenários diferentes, confirmados
    // com o dono do produto:
    //   1. Sinal OBRIGATÓRIO (agendamento nasce AGUARDANDO_PAGAMENTO — sem
    //      pagar o sinal, o horário não fica garantido).
    //   2. Pagamento ANTECIPADO OPCIONAL (agendamento já nasce AGENDADO
    //      normalmente, mas o estabelecimento aceita pagamento online e o
    //      cliente pode adiantar se quiser).

    public void enviarSolicitacaoPagamentoSinal(Agendamento agendamento) {
        if (!podeEnviar(agendamento)) return;

        String linkPagamento = baseUrl + "/app/"
                + agendamento.getEstabelecimento().getSlug()
                + "/pagamento/" + agendamento.getId();

        String mensagem = String.format(
                "Olá, %s! 👋\n\n" +
                        "Recebemos seu agendamento em *%s*:\n\n" +
                        "📋 *Serviço:* %s\n" +
                        "💇 *Profissional:* %s\n" +
                        "📅 *Data:* %s\n" +
                        "🕐 *Horário:* %s\n\n" +
                        "⚠️ Para garantir seu horário, é necessário pagar o sinal.\n\n" +
                        "💳 Pague aqui (Pix ou cartão):\n" +
                        "%s\n\n" +
                        "Assim que o pagamento for confirmado, seu agendamento fica garantido! 😊",
                agendamento.getCliente().getNome().split(" ")[0],
                agendamento.getEstabelecimento().getNome(),
                agendamento.getServico().getNome(),
                agendamento.getProfissional().getNome(),
                agendamento.getDataHoraInicio().format(FMT_DATA),
                agendamento.getDataHoraInicio().format(FMT_HORA),
                linkPagamento
        );

        enviarComLog(agendamento, mensagem, "SOLICITACAO_PAGAMENTO_SINAL");
    }

    public void enviarConvitePagamentoOpcional(Agendamento agendamento) {
        if (!podeEnviar(agendamento)) return;

        String linkPagamento = baseUrl + "/app/"
                + agendamento.getEstabelecimento().getSlug()
                + "/pagamento/" + agendamento.getId();

        String mensagem = String.format(
                "Olá, %s! Seu agendamento em *%s* já está confirmado. 😊\n\n" +
                        "Se preferir, você já pode adiantar o pagamento pelo Pix ou cartão:\n" +
                        "%s\n\n" +
                        "Se não quiser, sem problema — pode pagar direto no local!",
                agendamento.getCliente().getNome().split(" ")[0],
                agendamento.getEstabelecimento().getNome(),
                linkPagamento
        );

        enviarComLog(agendamento, mensagem, "CONVITE_PAGAMENTO_OPCIONAL");
    }

    // Enviado quando o webhook do Mercado Pago confirma status "approved"
    // para uma Transacao vinculada a um Agendamento. Chamado por
    // WebhookProcessorService.atualizarStatusTransacao — não é agendado, é
    // reativo ao evento de pagamento.
    public void enviarPagamentoAprovado(Agendamento agendamento) {
        if (!podeEnviar(agendamento)) return;

        String mensagem = String.format(
                "Olá, %s! ✅\n\n" +
                        "Recebemos a confirmação do seu pagamento para o agendamento em *%s*:\n\n" +
                        "📋 *Serviço:* %s\n" +
                        "📅 *Data:* %s às %s\n\n" +
                        "Seu horário está garantido! Te esperamos. 😊",
                agendamento.getCliente().getNome().split(" ")[0],
                agendamento.getEstabelecimento().getNome(),
                agendamento.getServico().getNome(),
                agendamento.getDataHoraInicio().format(FMT_DATA),
                agendamento.getDataHoraInicio().format(FMT_HORA)
        );

        enviarComLog(agendamento, mensagem, "PAGAMENTO_APROVADO");
    }
}