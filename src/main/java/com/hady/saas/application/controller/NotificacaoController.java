package com.hady.saas.application.controller;

import com.hady.saas.business.service.AgendamentoService;
import com.hady.saas.business.service.NotificacaoService;
import com.hady.saas.business.service.UsuarioService;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.usuario.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/painel/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final AgendamentoService agendamentoService;
    private final UsuarioService usuarioService;

    public NotificacaoController(NotificacaoService notificacaoService,
                                  AgendamentoService agendamentoService,
                                  UsuarioService usuarioService) {
        this.notificacaoService = notificacaoService;
        this.agendamentoService = agendamentoService;
        this.usuarioService = usuarioService;
    }

    // POST /painel/notificacoes/lembrete/{agendamentoId}
    // Envia lembrete manual para o cliente de um agendamento específico
    // Apenas ADMIN e FUNCIONARIO podem enviar
    @PostMapping("/lembrete/{agendamentoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<String> enviarLembrete(@PathVariable Long agendamentoId,
                                                  @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        Agendamento agendamento = agendamentoService.buscarPorId(
                agendamentoId, usuario.getEstabelecimento().getId());

        if (agendamento.getCliente().getEmail() == null ||
            agendamento.getCliente().getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Cliente não possui email cadastrado");
        }

        notificacaoService.enviarLembrete(agendamento);
        return ResponseEntity.ok("Lembrete enviado para " + agendamento.getCliente().getEmail());
    }
}
