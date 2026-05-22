package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.enums.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // Lista agendamentos de um profissional por período
    List<Agendamento> findByProfissionalIdAndDataHoraInicioBetween(
            Long profissionalId,
            LocalDateTime inicio,
            LocalDateTime fim
    );

    // Lista agendamentos do estabelecimento por período
    List<Agendamento> findByEstabelecimentoIdAndDataHoraInicioBetween(
            Long estabelecimentoId,
            LocalDateTime inicio,
            LocalDateTime fim
    );

    // Verifica conflito de horário para um profissional
    // Retorna agendamentos que se sobrepõem ao horário solicitado
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.profissional.id = :profissionalId
        AND a.status = 'AGENDADO'
        AND a.dataHoraInicio < :fim
        AND a.dataHoraFim > :inicio
    """)
    List<Agendamento> findConflitos(
            @Param("profissionalId") Long profissionalId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    // Lista agendamentos de um cliente
    List<Agendamento> findByClienteIdOrderByDataHoraInicioDesc(Long clienteId);

    // Lista agendamentos por status no estabelecimento
    List<Agendamento> findByEstabelecimentoIdAndStatus(Long estabelecimentoId, StatusAgendamento status);

    // Conta faltas de um cliente no estabelecimento
    int countByClienteIdAndStatus(Long clienteId, StatusAgendamento status);
}
