package com.hady.saas.infrastructure.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateLimitCleanupTask {

    private final RateLimitFilter rateLimitFilter;

    public RateLimitCleanupTask(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    // Única responsável pela limpeza periódica — o @Scheduled foi removido
    // do RateLimitFilter para evitar dupla execução concorrente a cada 5 min.
    @Scheduled(fixedDelay = 300_000)
    public void limpar() {
        rateLimitFilter.limparContadoresExpirados();
    }

}