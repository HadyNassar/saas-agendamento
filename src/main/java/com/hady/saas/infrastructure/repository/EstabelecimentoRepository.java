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

    // Autocomplete por nome — usado na barra de busca do mapa público.
    @Query(value = """
            SELECT * FROM estabelecimentos e
            WHERE e.ativo = true
              AND e.latitude IS NOT NULL
              AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
            ORDER BY e.nome ASC
            LIMIT 8
            """, nativeQuery = true)
    List<Estabelecimento> buscarPorNome(@Param("termo") String termo);
}