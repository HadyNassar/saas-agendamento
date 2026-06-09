package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.whatsapp.WhatsAppMensagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsAppMensagemRepository extends JpaRepository<WhatsAppMensagem, Long> {

    List<WhatsAppMensagem> findByEstabelecimentoIdOrderByCriadoEmDesc(Long estabelecimentoId);

    List<WhatsAppMensagem> findByAgendamentoId(Long agendamentoId);

    List<WhatsAppMensagem> findByStatus(String status);
}