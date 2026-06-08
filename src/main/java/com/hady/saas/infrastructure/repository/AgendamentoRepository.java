package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.enums.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // ── lista por estabelecimento e intervalo ──
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.cliente
        JOIN FETCH a.profissional
        JOIN FETCH a.servico
        WHERE a.estabelecimento.id = :estabelecimentoId
          AND a.dataHoraInicio BETWEEN :inicio AND :fim
        ORDER BY a.dataHoraInicio
    """)
    List<Agendamento> findByEstabelecimentoIdAndDataHoraInicioBetween(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    // ── lista por profissional e intervalo ──
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.cliente
        JOIN FETCH a.profissional
        JOIN FETCH a.servico
        WHERE a.profissional.id = :profissionalId
          AND a.dataHoraInicio BETWEEN :inicio AND :fim
        ORDER BY a.dataHoraInicio
    """)
    List<Agendamento> findByProfissionalIdAndDataHoraInicioBetween(
            @Param("profissionalId") Long profissionalId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    // ── conflitos de horário ──
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.profissional.id = :profissionalId
          AND a.status = com.hady.saas.domain.enums.StatusAgendamento.AGENDADO
          AND a.dataHoraInicio < :fim
          AND a.dataHoraFim > :inicio
    """)
    List<Agendamento> findConflitos(
            @Param("profissionalId") Long profissionalId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    // ── lembretes 24h / 2h ──
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.cliente
        JOIN FETCH a.profissional
        JOIN FETCH a.servico
        JOIN FETCH a.estabelecimento
        WHERE a.status = com.hady.saas.domain.enums.StatusAgendamento.AGENDADO
          AND a.dataHoraInicio BETWEEN :inicio AND :fim
    """)
    List<Agendamento> findAgendadosEntre(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    // ── busca por id com relacionamentos ──
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.cliente
        JOIN FETCH a.profissional
        JOIN FETCH a.servico
        JOIN FETCH a.estabelecimento
        WHERE a.id = :id
    """)
    Optional<Agendamento> findByIdWithDetails(@Param("id") Long id);

    // ── histórico do cliente ──
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.profissional
        JOIN FETCH a.servico
        WHERE a.cliente.id = :clienteId
          AND a.estabelecimento.id = :estabelecimentoId
        ORDER BY a.dataHoraInicio DESC
        LIMIT 20
    """)
    List<Agendamento> findTop20ByClienteIdOrderByDataHoraInicioDesc(
            @Param("clienteId") Long clienteId,
            @Param("estabelecimentoId") Long estabelecimentoId);

    // ── AVALIAÇÃO PÓS-ATENDIMENTO ─────────────────────────────────────────────
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.cliente
        JOIN FETCH a.profissional
        JOIN FETCH a.servico
        JOIN FETCH a.estabelecimento
        WHERE a.status = com.hady.saas.domain.enums.StatusAgendamento.CONCLUIDO
          AND a.avaliacaoEnviada = false
          AND a.dataHoraFim BETWEEN :inicio AND :fim
    """)
    List<Agendamento> findConcluidosSemAvaliacaoEntre(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    // ── LEMBRETE DE RETORNO ───────────────────────────────────────────────────
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.cliente c
        JOIN FETCH a.estabelecimento e
        JOIN FETCH a.servico
        WHERE a.status = com.hady.saas.domain.enums.StatusAgendamento.CONCLUIDO
          AND a.dataHoraFim BETWEEN :inicio AND :fim
          AND (c.ultimoLembreteRetorno IS NULL
               OR c.ultimoLembreteRetorno < :limiteEnvio)
          AND a.id = (
              SELECT MAX(a2.id) FROM Agendamento a2
              WHERE a2.cliente.id = c.id
                AND a2.estabelecimento.id = e.id
                AND a2.status = com.hady.saas.domain.enums.StatusAgendamento.CONCLUIDO
          )
    """)
    List<Agendamento> findClientesParaLembreteRetorno(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("limiteEnvio") LocalDateTime limiteEnvio);

    // ── RELATÓRIO FINANCEIRO ──────────────────────────────────────────────────
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.servico
        LEFT JOIN FETCH a.profissional
        WHERE a.estabelecimento.id = :estabelecimentoId
          AND a.status = :status
          AND a.dataHoraInicio BETWEEN :inicio AND :fim
    """)
    List<Agendamento> findByEstabelecimentoIdAndStatusAndDataHoraInicioBetween(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("status") StatusAgendamento status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
