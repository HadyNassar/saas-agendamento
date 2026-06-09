package com.hady.saas.webhook;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Serviço separado para processar eventos do webhook do Mercado Pago com
 * @Transactional funcionando corretamente.
 *
 * MOTIVO: @Transactional em métodos privados de um @RestController é ignorado
 * pelo Spring AOP (que usa proxy — chamadas privadas nunca passam pelo proxy).
 * Extraindo para um @Service separado, o Spring consegue criar o proxy e
 * garantir atomicidade nos saves.
 */
@Service
public class WebhookProcessorService {

    private static final Logger log = LoggerFactory.getLogger(WebhookProcessorService.class);

    private final EstabelecimentoRepository estabelecimentoRepo;
    private final TransacaoRepository transacaoRepo;

    public WebhookProcessorService(EstabelecimentoRepository estabelecimentoRepo,
                                   TransacaoRepository transacaoRepo) {
        this.estabelecimentoRepo = estabelecimentoRepo;
        this.transacaoRepo = transacaoRepo;
    }

    @Transactional
    public void atualizarStatusPlano(String subscriptionId, String status) {
        Optional<Estabelecimento> opt = estabelecimentoRepo.findByMpSubscriptionId(subscriptionId);
        opt.ifPresent(estab -> {
            if ("authorized".equals(status)) {
                estab.setPlanoAtivo(true);

                // Renova a partir da data atual de vencimento para não perder dias
                // caso o webhook chegue antes ou depois do vencimento exato.
                // Se planoValidoAte já passou (ou é null), renova a partir de hoje.
                LocalDate base = estab.getPlanoValidoAte();
                if (base == null || base.isBefore(LocalDate.now())) {
                    base = LocalDate.now();
                }
                estab.setPlanoValidoAte(base.plusMonths(1));

                log.info("[Webhook MP] Plano renovado — estabelecimento={} válido até={}",
                        estab.getId(), estab.getPlanoValidoAte());

            } else {
                // cancelled, paused, etc.
                estab.setPlanoAtivo(false);
                estab.setMpSubscriptionId(null);

                log.info("[Webhook MP] Plano desativado — estabelecimento={} status={}",
                        estab.getId(), status);
            }

            estabelecimentoRepo.save(estab);
        });
    }

    @Transactional
    public void atualizarStatusTransacao(String mpPaymentId, String mpStatus) {
        transacaoRepo.findByMpPaymentId(mpPaymentId).ifPresent(transacao -> {
            String novoStatus = switch (mpStatus) {
                case "approved"  -> "APROVADO";
                case "rejected"  -> "REJEITADO";
                case "refunded"  -> "REEMBOLSADO";
                case "cancelled" -> "CANCELADO";
                default          -> "PENDENTE";
            };
            transacao.setStatus(novoStatus);
            transacaoRepo.save(transacao);
            log.info("[Webhook MP] Transação atualizada — mpPaymentId={} status={}", mpPaymentId, novoStatus);
        });
    }
}
