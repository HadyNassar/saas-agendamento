package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Busca cliente por telefone dentro do estabelecimento
    Optional<Cliente> findByTelefoneAndEstabelecimentoId(String telefone, Long estabelecimentoId);

    // Lista clientes do estabelecimento
    List<Cliente> findByEstabelecimentoId(Long estabelecimentoId);

    // Verifica se cliente já existe no estabelecimento
    boolean existsByTelefoneAndEstabelecimentoId(String telefone, Long estabelecimentoId);
}
