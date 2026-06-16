package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.enums.StatusAgendamento;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job que cancela agendamentos que ficaram presos em AGUARDANDO_PAGAMENTO
 * quando o cliente abre o modal de pagamento mas abandona sem pagar.
 *
 * Roda a cada 2 minutos e cancela agendamentos com mais de 15 minutos
 * no status AGUARDANDO_PAGAMENTO.
 */
@Service
public class PagamentoPendenteCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PagamentoPendenteCleanupJob.class);

    // Tempo máximo que um agendamento pode ficar aguardando pagamento (em minutos)
    private static final int MINUTOS_EXPIRACAO = 15;

    private final AgendamentoRepository agendamentoRepo;

    public PagamentoPendenteCleanupJob(AgendamentoRepository agendamentoRepo) {
        this.agendamentoRepo = agendamentoRepo;
    }

    @Scheduled(fixedDelay = 2 * 60 * 1000) // roda a cada 2 minutos
    @Transactional
    public void cancelarPagamentosPendentesExpirados() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(MINUTOS_EXPIRACAO);

        List<Agendamento> expirados = agendamentoRepo
                .findAguardandoPagamentoAntesDe(limite);

        if (expirados.isEmpty()) return;

        log.info("[CleanupJob] Cancelando {} agendamento(s) com pagamento expirado", expirados.size());

        for (Agendamento ag : expirados) {
            ag.setStatus(StatusAgendamento.CANCELADO);
            agendamentoRepo.save(ag);
            log.info("[CleanupJob] Agendamento id={} estabelecimento={} cancelado por pagamento não realizado",
                    ag.getId(), ag.getEstabelecimento().getNome());
        }
    }
}