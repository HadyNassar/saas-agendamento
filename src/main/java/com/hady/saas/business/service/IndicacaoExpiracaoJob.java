package com.hady.saas.business.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Item 17 — expira links de indicação (Indicacao) que passaram dos 15 dias
 * de validade sem o indicado concluir o primeiro atendimento. Mesmo padrão
 * de PagamentoPendenteCleanupJob.
 */
@Service
public class IndicacaoExpiracaoJob {

    private static final Logger log = LoggerFactory.getLogger(IndicacaoExpiracaoJob.class);

    private final IndicacaoService indicacaoService;

    public IndicacaoExpiracaoJob(IndicacaoService indicacaoService) {
        this.indicacaoService = indicacaoService;
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000) // roda a cada 1 hora — expiração de 15 dias não é urgente por minuto
    public void expirarIndicacoesVencidas() {
        int total = indicacaoService.expirarPendentesVencidas();
        if (total > 0) {
            log.info("[IndicacaoExpiracaoJob] {} indicação(ões) marcada(s) como expirada(s)", total);
        }
    }
}