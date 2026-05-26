package com.hady.saas.application.controller;

import com.hady.saas.business.service.ClienteService;
import com.hady.saas.business.service.UsuarioService;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.usuario.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/painel/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final UsuarioService usuarioService;

    public ClienteController(ClienteService clienteService, UsuarioService usuarioService) {
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
    }

    // GET /painel/clientes
    @GetMapping
    public ResponseEntity<List<Cliente>> listar(@AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(clienteService.listar(usuario.getEstabelecimento().getId()));
    }

    // POST /painel/clientes — criar novo cliente pelo painel
    @PostMapping
    public ResponseEntity<Cliente> criar(@RequestBody Cliente dados,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        Cliente novo = clienteService.criar(dados, usuario.getEstabelecimento().getId());
        return ResponseEntity.ok(novo);
    }

    // PUT /painel/clientes/{id}
    @PutMapping("/{id}")
    public ResponseEntity<String> atualizar(@PathVariable Long id,
                                            @RequestBody Cliente dados,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        clienteService.atualizar(id, dados, usuario.getEstabelecimento().getId());
        return ResponseEntity.ok("Cliente atualizado!");
    }

    // PATCH /painel/clientes/{id}/bloquear — apenas ADMIN
    @PatchMapping("/{id}/bloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> bloquear(@PathVariable Long id,
                                           @RequestBody String motivo,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        clienteService.bloquear(id, motivo, usuario.getEstabelecimento().getId());
        return ResponseEntity.ok("Cliente bloqueado!");
    }

    // PATCH /painel/clientes/{id}/desbloquear — apenas ADMIN
    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> desbloquear(@PathVariable Long id,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        clienteService.desbloquear(id, usuario.getEstabelecimento().getId());
        return ResponseEntity.ok("Cliente desbloqueado!");
    }
}