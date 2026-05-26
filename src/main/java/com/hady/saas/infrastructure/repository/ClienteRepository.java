package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByEstabelecimentoId(Long estabelecimentoId);

    Optional<Cliente> findByTelefoneAndEstabelecimentoId(String telefone, Long estabelecimentoId);

    // ✅ evita LazyInitializationException no ClienteService
    Optional<Cliente> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);


}
