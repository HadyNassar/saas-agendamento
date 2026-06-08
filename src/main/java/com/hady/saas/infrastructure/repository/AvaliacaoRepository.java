package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.avaliacao.AvaliacaoAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvaliacaoRepository extends JpaRepository<AvaliacaoAtendimento, Long> {

    // Verifica se agendamento já foi avaliado
    boolean existsByAgendamentoId(Long agendamentoId);

    // Busca avaliação de um agendamento específico
    Optional<AvaliacaoAtendimento> findByAgendamentoId(Long agendamentoId);

    // Lista todas as avaliações do estabelecimento (painel — inclui ocultas)
    @Query("""
        SELECT a FROM AvaliacaoAtendimento a
        JOIN FETCH a.cliente
        JOIN FETCH a.agendamento ag
        JOIN FETCH ag.servico
        JOIN FETCH ag.profissional
        WHERE a.estabelecimento.id = :estabelecimentoId
        ORDER BY a.criadoEm DESC
    """)
    List<AvaliacaoAtendimento> findByEstabelecimentoIdOrderByCriadoEmDesc(
            @Param("estabelecimentoId") Long estabelecimentoId);

    // Lista apenas avaliações VISÍVEIS (página pública)
    @Query("""
        SELECT a FROM AvaliacaoAtendimento a
        JOIN FETCH a.cliente
        JOIN FETCH a.agendamento ag
        JOIN FETCH ag.servico
        JOIN FETCH ag.profissional
        WHERE a.estabelecimento.id = :estabelecimentoId
          AND a.oculta = false
        ORDER BY a.criadoEm DESC
    """)
    List<AvaliacaoAtendimento> findVisivelByEstabelecimentoId(
            @Param("estabelecimentoId") Long estabelecimentoId);

    // Média de notas do estabelecimento (só visíveis)
    @Query("""
        SELECT AVG(a.nota) FROM AvaliacaoAtendimento a
        WHERE a.estabelecimento.id = :estabelecimentoId
          AND a.oculta = false
    """)
    Double mediaNotasEstabelecimento(@Param("estabelecimentoId") Long estabelecimentoId);

    // Média por profissional (só visíveis)
    @Query("""
        SELECT AVG(a.nota) FROM AvaliacaoAtendimento a
        WHERE a.agendamento.profissional.id = :profissionalId
          AND a.oculta = false
    """)
    Double mediaNotasProfissional(@Param("profissionalId") Long profissionalId);

    // Verifica se cliente tem agendamento concluído sem avaliação (para pop-up)
    @Query("""
        SELECT COUNT(ag) > 0 FROM Agendamento ag
        WHERE ag.cliente.id = :clienteId
          AND ag.estabelecimento.id = :estabelecimentoId
          AND ag.status = com.hady.saas.domain.enums.StatusAgendamento.CONCLUIDO
          AND ag.avaliacaoEnviada = true
          AND NOT EXISTS (
              SELECT 1 FROM AvaliacaoAtendimento a
              WHERE a.agendamento.id = ag.id
          )
    """)
    boolean clienteTemAvaliacaoPendente(
            @Param("clienteId") Long clienteId,
            @Param("estabelecimentoId") Long estabelecimentoId);
}