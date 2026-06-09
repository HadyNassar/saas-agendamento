package com.hady.saas.application.controller;

import com.hady.saas.business.service.AgendamentoService;
import com.hady.saas.business.service.NotificacaoService;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/painel/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final AgendamentoService agendamentoService;
    private final AuthUtil authUtil;

    public NotificacaoController(NotificacaoService notificacaoService,
                                  AgendamentoService agendamentoService,
                                  AuthUtil authUtil) {
        this.notificacaoService = notificacaoService;
        this.agendamentoService = agendamentoService;
        this.authUtil = authUtil;
    }

    @PostMapping("/lembrete/{agendamentoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> enviarLembrete(@PathVariable Long agendamentoId, Authentication auth) {
        Agendamento agendamento = agendamentoService.buscarPorId(agendamentoId, authUtil.getEstabelecimentoId(auth));

        if (!agendamento.getEstabelecimento().isWhatsappConectado()) {
            return ResponseEntity.badRequest()
                    .body("WhatsApp não conectado. Acesse Configurações > WhatsApp para conectar.");
        }
        if (agendamento.getCliente().getTelefone() == null || agendamento.getCliente().getTelefone().isBlank()) {
            return ResponseEntity.badRequest().body("Cliente não possui telefone cadastrado.");
        }

        notificacaoService.enviarLembrete(agendamento);
        return ResponseEntity.ok("Lembrete enviado via WhatsApp para " + agendamento.getCliente().getTelefone());
    }
}
