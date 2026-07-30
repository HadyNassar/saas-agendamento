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

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Estabelecimento> findByMpSubscriptionId(String mpSubscriptionId);

    Optional<Estabelecimento> findByEvolutionInstanceName(String evolutionInstanceName);

    @Query(value = """
            SELECT * FROM estabelecimentos e
            WHERE e.ativo = true
              AND e.visivel_no_mapa = true
              AND e.latitude IS NOT NULL
              AND e.longitude IS NOT NULL
              AND (
                6371 * acos(
                    cos(radians(:lat)) * cos(radians(e.latitude::double precision)) *
                    cos(radians(e.longitude::double precision) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(e.latitude::double precision))
                )
              ) <= :raioKm
            ORDER BY (
                6371 * acos(
                    cos(radians(:lat)) * cos(radians(e.latitude::double precision)) *
                    cos(radians(e.longitude::double precision) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(e.latitude::double precision))
                )
            ) ASC
            """, nativeQuery = true)
    List<Estabelecimento> buscarProximos(@Param("lat") double lat,
                                         @Param("lng") double lng,
                                         @Param("raioKm") double raioKm);

    @Query(value = """
            SELECT * FROM estabelecimentos e
            WHERE e.ativo = true
              AND e.latitude IS NOT NULL
              AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
            ORDER BY e.nome ASC
            LIMIT 8
            """, nativeQuery = true)
    List<Estabelecimento> buscarPorNome(@Param("termo") String termo);

    // ── Admin master: listagem com filtros combináveis ──────────────────
    // Todos os parâmetros são opcionais (null = "não filtrar por isso").
    // status é calculado dinamicamente a partir de plano_ativo/trial_ativo,
    // não é uma coluna própria — por isso a lógica em CASE dentro do WHERE.
    @Query(value = """
            SELECT * FROM estabelecimentos e
            WHERE (:busca IS NULL OR :busca = '' OR LOWER(e.nome) LIKE LOWER(CONCAT('%', :busca, '%')))
              AND (:plano IS NULL OR :plano = '' OR e.plano = :plano)
              AND (:cidade IS NULL OR :cidade = '' OR LOWER(e.cidade) LIKE LOWER(CONCAT('%', :cidade, '%')))
              AND (
                    :status IS NULL OR :status = ''
                    OR (:status = 'ATIVO' AND e.plano_ativo = true AND e.plano_valido_ate >= CURRENT_DATE)
                    OR (:status = 'TRIAL' AND e.trial_ativo = true AND e.trial_fim >= CURRENT_DATE)
                    OR (:status = 'INATIVO' AND NOT (e.plano_ativo = true AND e.plano_valido_ate >= CURRENT_DATE)
                                              AND NOT (e.trial_ativo = true AND e.trial_fim >= CURRENT_DATE))
                  )
            ORDER BY e.criado_em DESC
            """, nativeQuery = true)
    List<Estabelecimento> buscarComFiltrosAdmin(@Param("busca") String busca,
                                                @Param("plano") String plano,
                                                @Param("cidade") String cidade,
                                                @Param("status") String status);
}