package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final WhatsAppService whatsAppService;
    private final LembreteConfirmacaoService lembreteConfirmacaoService;

    public NotificacaoService(AgendamentoRepository agendamentoRepository,
                              ClienteRepository clienteRepository,
                              WhatsAppService whatsAppService,
                              LembreteConfirmacaoService lembreteConfirmacaoService) {
        this.agendamentoRepository = agendamentoRepository;
        this.clienteRepository = clienteRepository;
        this.whatsAppService = whatsAppService;
        this.lembreteConfirmacaoService = lembreteConfirmacaoService;
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

    // ─── Scheduler: lembrete 24h antes ────────────────────────────────────────
    // Roda todo início de hora. Janela: 23h50 ~ 24h10 a partir de agora.

    @Scheduled(cron = "0 0 * * * *")
    public void enviarLembretes24h() {
        LocalDateTime agora  = LocalDateTime.now();
        LocalDateTime inicio = agora.plusHours(23).plusMinutes(50);
        LocalDateTime fim    = agora.plusHours(24).plusMinutes(10);

        List<Agendamento> agendamentos = agendamentoRepository.findAgendadosEntre(inicio, fim);
        log.info("[Lembrete 24h] {} agendamento(s) na janela {} ~ {}", agendamentos.size(), inicio, fim);

        for (Agendamento a : agendamentos) {
            try {
                // Item 2: só entra o prompt "responda 1/2" quando plano/robô/estado
                // da conversa permitem (ver LembreteConfirmacaoService.elegivel).
                // No STANDARD, ou com o robô pausado, o lembrete sai exatamente
                // como sempre foi — sem prompt nenhum.
                boolean elegivel = lembreteConfirmacaoService.elegivel(a);
                whatsAppService.enviarLembrete24h(a, elegivel);
                if (elegivel) {
                    lembreteConfirmacaoService.prepararConversaParaConfirmacao(a);
                }
            } catch (Exception e) {
                log.error("[Lembrete 24h] Erro ao enviar para agendamento id={}", a.getId(), e);
            }
        }
    }

    // ─── Scheduler: lembrete 2h antes ─────────────────────────────────────────
    // Roda a cada hora no minuto 15. Janela: 1h50 ~ 2h10 a partir de agora.

    @Scheduled(cron = "0 15 * * * *")
    public void enviarLembretes2h() {
        LocalDateTime agora  = LocalDateTime.now();
        LocalDateTime inicio = agora.plusHours(1).plusMinutes(50);
        LocalDateTime fim    = agora.plusHours(2).plusMinutes(10);

        List<Agendamento> agendamentos = agendamentoRepository.findAgendadosEntre(inicio, fim);
        log.info("[Lembrete 2h] {} agendamento(s) na janela {} ~ {}", agendamentos.size(), inicio, fim);

        for (Agendamento a : agendamentos) {
            try {
                boolean elegivel = lembreteConfirmacaoService.elegivel(a);
                whatsAppService.enviarLembrete2h(a, elegivel);
                if (elegivel) {
                    lembreteConfirmacaoService.prepararConversaParaConfirmacao(a);
                }
            } catch (Exception e) {
                log.error("[Lembrete 2h] Erro ao enviar para agendamento id={}", a.getId(), e);
            }
        }
    }

    // ─── Scheduler: avaliação pós-atendimento ─────────────────────────────────
    // Roda a cada hora no minuto 30.
    // Busca agendamentos CONCLUÍDOS entre 50 e 70 minutos atrás que ainda
    // não receberam o WhatsApp de avaliação.
    // CORRIGIDO (27/07/2026): mensagem transacional básica, disponível para
    // TODOS os planos, incluindo STANDARD — confirmado com o dono do produto
    // que só o robô conversacional/automação de marketing (CampanhaEnvio,
    // cupom, etc.) é exclusivo de ELITE/PLATINUM. Esta mensagem de pedido de
    // avaliação, assim como confirmação/lembretes/cancelamento, sempre
    // funcionou em todos os planos e o gating anterior (`!= ELITE/PLATINUM`)
    // estava incorreto — bloqueava STANDARD indevidamente.

    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void enviarAvaliacoesPosAtendimento() {
        LocalDateTime agora  = LocalDateTime.now();
        LocalDateTime inicio = agora.minusMinutes(70);
        LocalDateTime fim    = agora.minusMinutes(50);

        List<Agendamento> agendamentos =
                agendamentoRepository.findConcluidosSemAvaliacaoEntre(inicio, fim);

        log.info("[Avaliação] {} agendamento(s) para solicitar avaliação", agendamentos.size());

        for (Agendamento a : agendamentos) {
            try {
                whatsAppService.enviarSolicitacaoAvaliacao(a);
                a.setAvaliacaoEnviada(true);
                agendamentoRepository.save(a);

            } catch (Exception e) {
                log.error("[Avaliação] Erro ao enviar para agendamento id={}", a.getId(), e);
            }
        }
    }

    // ─── Scheduler: lembrete de retorno automático ────────────────────────────
    // Roda uma vez por dia às 10h.
    // Busca clientes cujo último atendimento concluído foi há ~30 dias
    // (janela: 29 a 31 dias atrás) e que não receberam lembrete nos últimos 25 dias.
    // CORRIGIDO (27/07/2026): mensagem transacional básica, disponível para
    // TODOS os planos, incluindo STANDARD — confirmado com o dono do produto
    // que só o robô conversacional/automação de marketing é exclusivo de
    // ELITE/PLATINUM. O gating anterior (`!= ELITE/PLATINUM`) estava
    // incorreto e bloqueava STANDARD indevidamente. Não confundir com o
    // gatilho automático de CAMPANHA por mudança de estágio (item 9/10/11 —
    // CampanhaAutomaticaService), que É exclusivo de ELITE/PLATINUM por ser
    // parte da automação de marketing, não uma notificação transacional.

    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void enviarLembretesRetorno() {
        LocalDateTime agora       = LocalDateTime.now();
        LocalDateTime inicio      = agora.minusDays(31);
        LocalDateTime fim         = agora.minusDays(29);
        LocalDateTime limiteEnvio = agora.minusDays(25);

        List<Agendamento> agendamentos =
                agendamentoRepository.findClientesParaLembreteRetorno(inicio, fim, limiteEnvio);

        log.info("[Retorno] {} cliente(s) para lembrete de retorno", agendamentos.size());

        for (Agendamento a : agendamentos) {
            try {
                whatsAppService.enviarLembreteRetorno(a);

                // Atualiza timestamp para evitar reenvio
                a.getCliente().setUltimoLembreteRetorno(agora);
                clienteRepository.save(a.getCliente());

            } catch (Exception e) {
                log.error("[Retorno] Erro ao enviar para cliente id={}", a.getCliente().getId(), e);
            }
        }
    }
}