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
    // FIX (bug 4): inclui AGUARDANDO_PAGAMENTO além de AGENDADO.
    // Sem isso, dois clientes podiam reservar o mesmo horário simultaneamente
    // quando o pagamento com sinal estava ativo — o segundo agendamento era criado
    // antes do primeiro ser confirmado, gerando double booking.
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.profissional.id = :profissionalId
          AND a.status IN (
            com.hady.saas.domain.enums.StatusAgendamento.AGENDADO,
            com.hady.saas.domain.enums.StatusAgendamento.AGUARDANDO_PAGAMENTO
          )
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

    // ── CLEANUP: agendamentos aguardando pagamento expirados ──────────────────
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.estabelecimento
        WHERE a.status = com.hady.saas.domain.enums.StatusAgendamento.AGUARDANDO_PAGAMENTO
          AND a.criadoEm < :limite
    """)
    List<Agendamento> findAguardandoPagamentoAntesDe(@Param("limite") LocalDateTime limite);

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

    // ── REAGENDAMENTO SELF-SERVICE (item 3) ───────────────────────────────────
    // Próximos agendamentos futuros e ainda ativos (AGENDADO ou AGUARDANDO_PAGAMENTO)
    // do cliente, no estabelecimento, ordenados por data. Usado pelo bot pra saber
    // se tem 0, 1 ou N agendamentos elegíveis a reagendar quando o cliente pede
    // "remarcar" fora de qualquer fluxo (estado LIVRE).
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.profissional
        JOIN FETCH a.servico
        WHERE a.cliente.id = :clienteId
          AND a.estabelecimento.id = :estabelecimentoId
          AND a.status IN (
            com.hady.saas.domain.enums.StatusAgendamento.AGENDADO,
            com.hady.saas.domain.enums.StatusAgendamento.AGUARDANDO_PAGAMENTO
          )
          AND a.dataHoraInicio > :agora
        ORDER BY a.dataHoraInicio
    """)
    List<Agendamento> findFuturosAtivosPorCliente(
            @Param("clienteId") Long clienteId,
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("agora") LocalDateTime agora);

    // ── SEGMENTAÇÃO DE CLIENTE (EstagioCliente) ───────────────────────────────
    // Retorna [0]=total de visitas concluídas, [1]=ticket médio, [2]=data da última visita
    @Query("""
        SELECT COUNT(a), AVG(s.preco), MAX(a.dataHoraFim)
        FROM Agendamento a JOIN a.servico s
        WHERE a.cliente.id = :clienteId
          AND a.estabelecimento.id = :estabelecimentoId
          AND a.status = com.hady.saas.domain.enums.StatusAgendamento.CONCLUIDO
    """)
    Object[] buscarEstatisticasCliente(@Param("clienteId") Long clienteId,
                                       @Param("estabelecimentoId") Long estabelecimentoId);

    // ── ITEM 12 (PLATINUM/ELITE) — Métricas de WhatsApp ───────────────────────
    // Todos os agendamentos criados por um estabelecimento numa janela de tempo
    // (usa criadoEm, não dataHoraInicio, porque a heurística "via campanha"
    // precisa saber QUANDO o agendamento foi feito em relação ao envio da
    // campanha, não quando o atendimento vai acontecer).
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.estabelecimento.id = :estabelecimentoId
          AND a.cliente IS NOT NULL
          AND a.criadoEm BETWEEN :inicio AND :fim
    """)
    List<Agendamento> findCriadosEntrePorEstabelecimento(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    // Agendamentos com status AGENDADO cujo período de atendimento (dataHoraInicio)
    // caiu dentro do intervalo do relatório — usado para medir a taxa de
    // confirmação (confirmadoPeloCliente) e o resultado dos lembretes 24h/2h
    // (quantos desses viraram CONCLUIDO vs CANCELADO/FALTOU). Reaproveita o
    // mesmo filtro de status de findAgendadosEntre, mas sem o JOIN FETCH
    // pesado (aqui só contamos, não exibimos detalhe).
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.estabelecimento.id = :estabelecimentoId
          AND a.dataHoraInicio BETWEEN :inicio AND :fim
    """)
    List<Agendamento> findPorEstabelecimentoEPeriodoAtendimento(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
    @Query("""
        SELECT COUNT(a) > 0 FROM Agendamento a
        WHERE a.cliente.id = :clienteId
          AND a.status = com.hady.saas.domain.enums.StatusAgendamento.CONCLUIDO
    """)
    boolean clienteJaTeveAtendimentoConcluido(@Param("clienteId") Long clienteId);
}