package com.hady.saas.application.controller;

import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.domain.whatsapp.WhatsAppMensagem;
import com.hady.saas.business.service.UsuarioService;
import com.hady.saas.infrastructure.repository.WhatsAppMensagemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GET /painel/whatsapp/mensagens → histórico de mensagens enviadas pelo estabelecimento
 * GET /painel/whatsapp/mensagens/agendamento/{id} → mensagens de um agendamento específico
 */
@RestController
@RequestMapping("/painel/whatsapp/mensagens")
public class WhatsAppMensagemController {

    private final WhatsAppMensagemRepository mensagemRepository;
    private final UsuarioService usuarioService;

    public WhatsAppMensagemController(WhatsAppMensagemRepository mensagemRepository,
                                      UsuarioService usuarioService) {
        this.mensagemRepository = mensagemRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<WhatsAppMensagem>> listar(
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        Long estabelecimentoId = usuario.getEstabelecimento().getId();

        List<WhatsAppMensagem> mensagens = mensagemRepository
                .findByEstabelecimentoIdOrderByCriadoEmDesc(estabelecimentoId);

        return ResponseEntity.ok(mensagens);
    }

    @GetMapping("/agendamento/{agendamentoId}")
    public ResponseEntity<List<WhatsAppMensagem>> porAgendamento(
            @PathVariable Long agendamentoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<WhatsAppMensagem> mensagens = mensagemRepository
                .findByAgendamentoId(agendamentoId);

        return ResponseEntity.ok(mensagens);
    }
}