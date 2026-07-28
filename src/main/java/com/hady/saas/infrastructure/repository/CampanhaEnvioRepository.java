package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.whatsapp.CampanhaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampanhaEnvioRepository extends JpaRepository<CampanhaEnvio, Long> {

    List<CampanhaEnvio> findByEstabelecimentoIdOrderByCriadoEmDesc(Long estabelecimentoId);

    // Usado pelo controller de campanha manual (seção 5-B) pra garantir que o
    // dono só enxerga/mexe nas campanhas do próprio estabelecimento, mesmo
    // sabendo o id de uma campanha de outro salão.
    Optional<CampanhaEnvio> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);

    @Query("""
        SELECT c FROM CampanhaEnvio c
        WHERE c.status = 'CRIADA'
        ORDER BY c.criadoEm ASC
    """)
    List<CampanhaEnvio> buscarPendentesDeProcessamento();
}