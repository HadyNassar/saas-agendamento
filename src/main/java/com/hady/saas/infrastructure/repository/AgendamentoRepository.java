package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.agendamento.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // ── lista por estabelecimento e intervalo — JOIN FETCH para evitar LazyInitializationException ──
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

    // ── lista por profissional e intervalo — JOIN FETCH ──
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

    // ── conflitos de horário (sem JOIN FETCH — só verifica existência) ──
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

    // ── lembretes agendados (para o scheduler) ──
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.cliente
        JOIN FETCH a.profissional
        JOIN FETCH a.servico
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
}
