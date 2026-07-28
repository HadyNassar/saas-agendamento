package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.alerta.AlertaPainel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaPainelRepository extends JpaRepository<AlertaPainel, Long> {

    // Lista alertas não vistos do estabelecimento (badge/lista ao abrir o painel)
    List<AlertaPainel> findByEstabelecimentoIdAndVistoFalseOrderByCriadoEmDesc(Long estabelecimentoId);

    // Histórico completo (vistos e não vistos), mais recente primeiro
    List<AlertaPainel> findByEstabelecimentoIdOrderByCriadoEmDesc(Long estabelecimentoId);

    @Query("SELECT COUNT(a) FROM AlertaPainel a WHERE a.estabelecimento.id = :estabelecimentoId AND a.visto = false")
    long contarNaoVistos(@Param("estabelecimentoId") Long estabelecimentoId);
}