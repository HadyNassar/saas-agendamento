package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {

    Optional<Estabelecimento> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Necessário para gerar slug único ao EDITAR um estabelecimento já existente:
    // exclui o próprio registro da checagem, senão o slug atual dele sempre
    // "colidiria consigo mesmo" e ganharia sufixo -1, -2... a cada salvamento.
    boolean existsBySlugAndIdNot(String slug, Long id);

    // Necessário para o MercadoPagoWebhook atualizar status da assinatura
    Optional<Estabelecimento> findByMpSubscriptionId(String mpSubscriptionId);

    // ── Mapa público ──────────────────────────────────────────────────────
    // Busca estabelecimentos ativos com coordenadas dentro de um raio (km),
    // usando a fórmula de Haversine direto no Postgres. Retorna ordenado do
    // mais próximo pro mais distante.
    @Query(value = """
            SELECT * FROM estabelecimentos e
            WHERE e.ativo = true
              AND e.latitude IS NOT NULL
              AND e.longitude IS NOT NULL
              AND (
                6371 * acos(
                    cos(radians(:lat)) * cos(radians(e.latitude)) *
                    cos(radians(e.longitude) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(e.latitude))
                )
              ) <= :raioKm
            ORDER BY (
                6371 * acos(
                    cos(radians(:lat)) * cos(radians(e.latitude)) *
                    cos(radians(e.longitude) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(e.latitude))
                )
            ) ASC
            """, nativeQuery = true)
    List<Estabelecimento> buscarProximos(@Param("lat") double lat,
                                         @Param("lng") double lng,
                                         @Param("raioKm") double raioKm);
}