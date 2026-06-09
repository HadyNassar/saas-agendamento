package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// SUBSTITUI o EstabelecimentoRepository existente
// Único acréscimo: findByMpSubscriptionId — usado pelo webhook do Mercado Pago
@Repository
public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {

    Optional<Estabelecimento> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Novo — necessário para o MercadoPagoWebhook atualizar status da assinatura
    Optional<Estabelecimento> findByMpSubscriptionId(String mpSubscriptionId);
}
