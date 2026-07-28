package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.whatsapp.WhatsAppMensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WhatsAppMensagemRepository extends JpaRepository<WhatsAppMensagem, Long> {

    List<WhatsAppMensagem> findByEstabelecimentoIdOrderByCriadoEmDesc(Long estabelecimentoId);

    List<WhatsAppMensagem> findByAgendamentoId(Long agendamentoId);

    // Item 5 (camada ELITE): histórico de mensagens de uma conversa específica,
    // em ordem cronológica (mais antiga primeiro) — o jeito natural de exibir
    // um inbox de chat.
    List<WhatsAppMensagem> findByConversaIdOrderByCriadoEmAsc(Long conversaId);

    List<WhatsAppMensagem> findByStatus(String status);

    // ── ITEM 12 (PLATINUM/ELITE) — Métricas de WhatsApp ────────────────────
    // Todos os envios de campanha (tipo=CAMPANHA, status=ENVIADO) de um
    // estabelecimento numa janela de tempo, usados pela heurística de
    // "agendamento via campanha" (checa se algum agendamento novo do cliente
    // caiu até N dias depois de um destes envios).
    @Query("""
        SELECT m FROM WhatsAppMensagem m
        WHERE m.estabelecimento.id = :estabelecimentoId
          AND m.tipo = 'CAMPANHA'
          AND m.status = 'ENVIADO'
          AND m.cliente IS NOT NULL
          AND m.enviadoEm BETWEEN :desde AND :ate
        ORDER BY m.cliente.id, m.enviadoEm
    """)
    List<WhatsAppMensagem> findCampanhasEnviadasParaMetricas(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("desde") LocalDateTime desde,
            @Param("ate") LocalDateTime ate);
}