package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.cupom.CupomEstabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CupomEstabelecimentoRepository extends JpaRepository<CupomEstabelecimento, Long> {

    List<CupomEstabelecimento> findByEstabelecimentoIdOrderByCriadoEmDesc(Long estabelecimentoId);

    Optional<CupomEstabelecimento> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);

    // Usado tanto na criação (checar duplicidade) quanto na aplicação via
    // link público — código é único por estabelecimento, não globalmente.
    Optional<CupomEstabelecimento> findByEstabelecimentoIdAndCodigoIgnoreCase(Long estabelecimentoId, String codigo);
}