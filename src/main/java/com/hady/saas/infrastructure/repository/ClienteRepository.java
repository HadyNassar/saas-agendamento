package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.cliente.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByEstabelecimentoId(Long estabelecimentoId);

    Optional<Cliente> findByTelefoneAndEstabelecimentoId(String telefone, Long estabelecimentoId);

    Optional<Cliente> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);

    Optional<Cliente> findByIdAndEstabelecimentoSlug(Long id, String slug);

    // ─── Paginação sem busca ─────────────────────────────────────────────────
    Page<Cliente> findByEstabelecimentoId(Long estabelecimentoId, Pageable pageable);

    // ─── Busca unificada: nome, telefone ou e-mail ───────────────────────────
    @Query("""
            SELECT c FROM Cliente c
            WHERE c.estabelecimento.id = :estabelecimentoId
              AND (
                LOWER(c.nome)   LIKE LOWER(CONCAT('%', :busca, '%')) OR
                c.telefone      LIKE CONCAT('%', :busca, '%')        OR
                LOWER(c.email)  LIKE LOWER(CONCAT('%', :busca, '%'))
              )
            """)
    Page<Cliente> buscarPorNomeTelefoneOuEmail(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("busca") String busca,
            Pageable pageable);

    // ─── Filtro por bloqueados ───────────────────────────────────────────────
    Page<Cliente> findByEstabelecimentoIdAndBloqueado(
            Long estabelecimentoId, boolean bloqueado, Pageable pageable);
}