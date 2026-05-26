package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.enums.StatusAgendamento;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificacaoService {

    private final AgendamentoRepository agendamentoRepository;
    private final WhatsAppService whatsAppService;

    public NotificacaoService(AgendamentoRepository agendamentoRepository,
                              WhatsAppService whatsAppService) {
        this.agendamentoRepository = agendamentoRepository;
        this.whatsAppService = whatsAppService;
    }

    // ─── Confirmação ──────────────────────────────────────────────────────────

    public void enviarConfirmacao(Agendamento agendamento) {
        whatsAppService.enviarConfirmacao(agendamento);
    }

    // ─── Lembrete manual ──────────────────────────────────────────────────────

    public void enviarLembrete(Agendamento agendamento) {
        whatsAppService.enviarLembrete(agendamento);
    }

    // ─── Cancelamento ─────────────────────────────────────────────────────────

    public void enviarCancelamento(Agendamento agendamento) {
        whatsAppService.enviarCancelamento(agendamento);
    }

    // ─── Falta ────────────────────────────────────────────────────────────────

    public void enviarFalta(Agendamento agendamento) {
        whatsAppService.enviarFalta(agendamento);
    }

    // ─── Scheduler: lembrete 24h antes — roda todo início de hora ────────────
    // FIX: cron separado do scheduler de 2h para não disputar recursos

    @Scheduled(cron = "0 0 * * * *")
    public void enviarLembretes24h() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicio = agora.plusHours(23).plusMinutes(50);
        LocalDateTime fim    = agora.plusHours(24).plusMinutes(10);

        agendamentoRepository.findAll().stream()
                .filter(a -> a.getStatus() == StatusAgendamento.AGENDADO)
                .filter(a -> a.getDataHoraInicio().isAfter(inicio)
                        && a.getDataHoraInicio().isBefore(fim))
                .forEach(a -> {
                    try {
                        whatsAppService.enviarLembrete24h(a);
                    } catch (Exception e) {
                        System.err.println("Erro lembrete 24h agendamento " + a.getId() + ": " + e.getMessage());
                    }
                });
    }

    // ─── Scheduler: lembrete 2h antes — roda a cada 15 minutos ──────────────
    // FIX: cron diferente do anterior para evitar execução simultânea

    @Scheduled(cron = "0 15 * * * *")
    public void enviarLembretes2h() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicio = agora.plusHours(1).plusMinutes(50);
        LocalDateTime fim    = agora.plusHours(2).plusMinutes(10);

        agendamentoRepository.findAll().stream()
                .filter(a -> a.getStatus() == StatusAgendamento.AGENDADO)
                .filter(a -> a.getDataHoraInicio().isAfter(inicio)
                        && a.getDataHoraInicio().isBefore(fim))
                .forEach(a -> {
                    try {
                        whatsAppService.enviarLembrete2h(a);
                    } catch (Exception e) {
                        System.err.println("Erro lembrete 2h agendamento " + a.getId() + ": " + e.getMessage());
                    }
                });
    }
}
