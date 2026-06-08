package com.hady.saas.application.controller;

import com.hady.saas.business.service.AgendamentoService;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.whatsapp.WhatsAppMensagem;
import com.hady.saas.infrastructure.repository.WhatsAppMensagemRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/painel/whatsapp/mensagens")
public class WhatsAppMensagemController {

    private final WhatsAppMensagemRepository mensagemRepository;
    private final AgendamentoService agendamentoService;
    private final AuthUtil authUtil;

    public WhatsAppMensagemController(WhatsAppMensagemRepository mensagemRepository,
                                      AgendamentoService agendamentoService,
                                      AuthUtil authUtil) {
        this.mensagemRepository = mensagemRepository;
        this.agendamentoService = agendamentoService;
        this.authUtil = authUtil;
    }

    @GetMapping
    public ResponseEntity<List<WhatsAppMensagem>> listar(Authentication auth) {
        return ResponseEntity.ok(
            mensagemRepository.findByEstabelecimentoIdOrderByCriadoEmDesc(authUtil.getEstabelecimentoId(auth))
        );
    }

    @GetMapping("/agendamento/{agendamentoId}")
    public ResponseEntity<List<WhatsAppMensagem>> porAgendamento(@PathVariable Long agendamentoId,
                                                                  Authentication auth) {
        // Valida que o agendamento pertence ao estabelecimento do usuário autenticado.
        // Sem isso, qualquer funcionário autenticado poderia consultar mensagens de
        // agendamentos de outros tenants passando um agendamentoId arbitrário (IDOR).
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        agendamentoService.buscarPorId(agendamentoId, estabelecimentoId);

        return ResponseEntity.ok(mensagemRepository.findByAgendamentoId(agendamentoId));
    }
}
