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

    // Necessário para gerar slug único ao EDITAR um estabelecimento já existente:
    // exclui o próprio registro da checagem, senão o slug atual dele sempre
    // "colidiria consigo mesmo" e ganharia sufixo -1, -2... a cada salvamento.
    boolean existsBySlugAndIdNot(String slug, Long id);

    // Novo — necessário para o MercadoPagoWebhook atualizar status da assinatura
    Optional<Estabelecimento> findByMpSubscriptionId(String mpSubscriptionId);
}