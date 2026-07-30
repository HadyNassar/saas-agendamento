package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.enums.EstagioCliente;
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

    // ─── Usado pelo filtro de campanha TODOS (disparo pra toda a base) ──────
    List<Cliente> findByEstabelecimentoIdAndBloqueadoFalse(Long estabelecimentoId);

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

    // ─── Filtro por estágio persistido (migration V23) ───────────────────────
    // Usa o snapshot em Cliente.estagioAtual — pensado para consumo futuro
    // pelos itens 9/10 (campanha segmentada / reativação por regras), que
    // precisam listar "todos os VIP" ou "todos os INATIVO" sem recalcular
    // cliente a cliente. Ainda sem consumidor nesta etapa.
    List<Cliente> findByEstabelecimentoIdAndEstagioAtual(Long estabelecimentoId, EstagioCliente estagioAtual);

    // ── ITEM 12 (PLATINUM/ELITE) — Métricas de WhatsApp ───────────────────────
    // Clientes que receberam o lembrete de retorno pós-atendimento
    // (WhatsAppService / findClientesParaLembreteRetorno) dentro da janela do
    // relatório — usado para medir quantos deles voltaram a agendar depois.
    @Query("""
        SELECT c FROM Cliente c
        WHERE c.estabelecimento.id = :estabelecimentoId
          AND c.ultimoLembreteRetorno BETWEEN :inicio AND :fim
    """)
    List<Cliente> findComLembreteRetornoEntre(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("inicio") java.time.LocalDateTime inicio,
            @Param("fim") java.time.LocalDateTime fim);
}