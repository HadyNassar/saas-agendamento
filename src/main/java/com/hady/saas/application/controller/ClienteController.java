package com.hady.saas.application.controller;

import com.hady.saas.application.dto.EstatisticasClienteDTO;
import com.hady.saas.business.service.ClienteSegmentacaoService;
import com.hady.saas.business.service.ClienteService;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/painel/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final AuthUtil authUtil;
    private final ClienteSegmentacaoService clienteSegmentacaoService;

    public ClienteController(ClienteService clienteService, AuthUtil authUtil,
                             ClienteSegmentacaoService clienteSegmentacaoService) {
        this.clienteService = clienteService;
        this.authUtil = authUtil;
        this.clienteSegmentacaoService = clienteSegmentacaoService;
    }

    @GetMapping
    public ResponseEntity<List<Cliente>> listar(Authentication auth) {
        return ResponseEntity.ok(clienteService.listar(authUtil.getEstabelecimentoId(auth)));
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<Cliente>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String busca,
            Authentication auth) {
        return ResponseEntity.ok(
                clienteService.listarPaginado(authUtil.getEstabelecimentoId(auth), page, size, busca)
        );
    }

    @GetMapping("/{id}/estagio")
    public ResponseEntity<EstatisticasClienteDTO> estagio(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(
                clienteSegmentacaoService.calcular(id, authUtil.getEstabelecimentoId(auth))
        );
    }

    @PostMapping
    public ResponseEntity<Cliente> criar(@RequestBody Cliente dados, Authentication auth) {
        return ResponseEntity.ok(clienteService.criar(dados, authUtil.getEstabelecimentoId(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> atualizar(@PathVariable Long id,
                                            @RequestBody Cliente dados,
                                            Authentication auth) {
        clienteService.atualizar(id, dados, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.ok("Cliente atualizado!");
    }

    @PatchMapping("/{id}/bloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> bloquear(@PathVariable Long id,
                                           @RequestBody String motivo,
                                           Authentication auth) {
        clienteService.bloquear(id, motivo, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.ok("Cliente bloqueado!");
    }

    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> desbloquear(@PathVariable Long id, Authentication auth) {
        clienteService.desbloquear(id, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.ok("Cliente desbloqueado!");
    }
}