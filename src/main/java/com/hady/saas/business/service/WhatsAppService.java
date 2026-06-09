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

    // ─── Lembrete 24h ────────────────────────────────────────────────────────

    public void enviarLembrete24h(Agendamento agendamento) {
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

        enviarComLog(agendamento, mensagem, "LEMBRETE_24H");
    }

    // ─── Lembrete 2h ─────────────────────────────────────────────────────────

    public void enviarLembrete2h(Agendamento agendamento) {
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
}
