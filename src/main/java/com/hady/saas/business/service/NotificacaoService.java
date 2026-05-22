package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.enums.StatusAgendamento;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificacaoService {

    private final EmailService emailService;
    private final AgendamentoRepository agendamentoRepository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    public NotificacaoService(EmailService emailService,
                               AgendamentoRepository agendamentoRepository) {
        this.emailService = emailService;
        this.agendamentoRepository = agendamentoRepository;
    }

    // Envia email de confirmação quando agendamento é criado
    public void enviarConfirmacao(Agendamento agendamento) {
        String email = agendamento.getCliente().getEmail();

        // Só envia se o cliente tiver email cadastrado
        if (email == null || email.isBlank()) return;

        String dataHora = agendamento.getDataHoraInicio().format(FORMATTER);
        String corpo = emailService.templateConfirmacao(
                agendamento.getCliente().getNome(),
                agendamento.getProfissional().getNome(),
                agendamento.getServico().getNome(),
                agendamento.getEstabelecimento().getNome(),
                dataHora
        );

        emailService.enviar(
                email,
                "✅ Agendamento confirmado - " + agendamento.getEstabelecimento().getNome(),
                corpo
        );
    }

    // Envia lembrete manual para um agendamento específico
    public void enviarLembrete(Agendamento agendamento) {
        String email = agendamento.getCliente().getEmail();

        if (email == null || email.isBlank()) return;

        String dataHora = agendamento.getDataHoraInicio().format(FORMATTER);
        String corpo = emailService.templateLembrete(
                agendamento.getCliente().getNome(),
                agendamento.getProfissional().getNome(),
                agendamento.getServico().getNome(),
                agendamento.getEstabelecimento().getNome(),
                dataHora
        );

        emailService.enviar(
                email,
                "🔔 Lembrete de agendamento - " + agendamento.getEstabelecimento().getNome(),
                corpo
        );
    }

    // Roda automaticamente a cada hora
    // Envia lembrete 24h antes do agendamento
    @Scheduled(cron = "0 0 * * * *") // todo início de hora
    public void enviarLembretes24h() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicio = agora.plusHours(23).plusMinutes(50);
        LocalDateTime fim = agora.plusHours(24).plusMinutes(10);

        List<Agendamento> agendamentos = agendamentoRepository
                .findByEstabelecimentoIdAndDataHoraInicioBetween(null, inicio, fim);

        // Busca todos os agendamentos no intervalo de 24h
        agendamentoRepository.findAll().stream()
                .filter(a -> a.getStatus() == StatusAgendamento.AGENDADO)
                .filter(a -> a.getDataHoraInicio().isAfter(inicio) &&
                             a.getDataHoraInicio().isBefore(fim))
                .forEach(a -> {
                    try {
                        enviarLembrete(a);
                    } catch (Exception e) {
                        // Log do erro mas não interrompe os outros lembretes
                        System.err.println("Erro ao enviar lembrete 24h para agendamento " +
                                a.getId() + ": " + e.getMessage());
                    }
                });
    }

    // Roda automaticamente a cada hora
    // Envia lembrete 2h antes do agendamento
    @Scheduled(cron = "0 0 * * * *") // todo início de hora
    public void enviarLembretes2h() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicio = agora.plusHours(1).plusMinutes(50);
        LocalDateTime fim = agora.plusHours(2).plusMinutes(10);

        agendamentoRepository.findAll().stream()
                .filter(a -> a.getStatus() == StatusAgendamento.AGENDADO)
                .filter(a -> a.getDataHoraInicio().isAfter(inicio) &&
                             a.getDataHoraInicio().isBefore(fim))
                .forEach(a -> {
                    try {
                        enviarLembrete(a);
                    } catch (Exception e) {
                        System.err.println("Erro ao enviar lembrete 2h para agendamento " +
                                a.getId() + ": " + e.getMessage());
                    }
                });
    }
}
