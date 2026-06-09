package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.anamnese.RespostaAnamnese;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RespostaAnamneseRepository extends JpaRepository<RespostaAnamnese, Long> {

    List<RespostaAnamnese> findByClienteId(Long clienteId);

    Optional<RespostaAnamnese> findByClienteIdAndPerguntaId(Long clienteId, Long perguntaId);

    boolean existsByClienteId(Long clienteId);

    @Modifying
    @Query("DELETE FROM RespostaAnamnese r WHERE r.pergunta.id = :perguntaId")
    void deleteByPerguntaId(@Param("perguntaId") Long perguntaId);

    @Query("""
        SELECT r FROM RespostaAnamnese r
        JOIN FETCH r.pergunta
        WHERE r.cliente.id = :clienteId
          AND r.pergunta.estabelecimento.id = :estabelecimentoId
    """)
    List<RespostaAnamnese> findByClienteIdAndEstabelecimentoId(
            @Param("clienteId") Long clienteId,
            @Param("estabelecimentoId") Long estabelecimentoId);

    @Query("""
        SELECT r FROM RespostaAnamnese r
        JOIN FETCH r.pergunta
        WHERE r.cliente.id = :clienteId
          AND r.pergunta.estabelecimento.slug = :slug
    """)
    List<RespostaAnamnese> findByClienteIdAndEstabelecimentoSlug(
            @Param("clienteId") Long clienteId,
            @Param("slug") String slug);
}