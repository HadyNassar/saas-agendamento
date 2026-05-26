package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.whatsapp.WhatsAppMensagem;
import com.hady.saas.infrastructure.client.EvolutionApiClient;
import com.hady.saas.infrastructure.repository.WhatsAppMensagemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class WhatsAppService {

    private final EvolutionApiClient evolutionApiClient;
    private final WhatsAppMensagemRepository mensagemRepository;

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
        WhatsAppMensagem log = new WhatsAppMensagem();
        log.setEstabelecimento(agendamento.getEstabelecimento());
        log.setAgendamento(agendamento);
        log.setCliente(agendamento.getCliente());
        log.setTelefoneDestino(agendamento.getCliente().getTelefone());
        log.setMensagem(mensagem);
        log.setTipo(tipo);

        try {
            evolutionApiClient.enviarMensagem(
                    agendamento.getEstabelecimento().getEvolutionInstanceName(),
                    agendamento.getCliente().getTelefone(),
                    mensagem
            );
            log.setStatus("ENVIADO");
            log.setEnviadoEm(LocalDateTime.now());
        } catch (Exception e) {
            log.setStatus("ERRO");
            log.setErroDetalhe(e.getMessage());
            System.err.println("[WhatsApp] Erro ao enviar (" + tipo + "): " + e.getMessage());
        }

        mensagemRepository.save(log);
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

    // ─── Lembrete 24h (chamado pelo scheduler) ────────────────────────────────

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

    // ─── Lembrete 2h (chamado pelo scheduler) ────────────────────────────────

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

    // ─── Lembrete manual (botão no painel) ───────────────────────────────────

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
}