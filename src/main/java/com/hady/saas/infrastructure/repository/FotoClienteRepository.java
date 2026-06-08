package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.fotocliente.FotoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FotoClienteRepository extends JpaRepository<FotoCliente, Long> {

    List<FotoCliente> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    List<FotoCliente> findByClienteIdAndClienteEstabelecimentoIdOrderByCriadoEmDesc(
            Long clienteId, Long estabelecimentoId);

    long countByClienteId(Long clienteId);
}
