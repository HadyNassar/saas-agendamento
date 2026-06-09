package com.hady.saas.application.controller;

import com.hady.saas.application.dto.UsuarioMeDTO;
import com.hady.saas.business.service.UsuarioService;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthUtil authUtil;

    public UsuarioController(UsuarioService usuarioService, AuthUtil authUtil) {
        this.usuarioService = usuarioService;
        this.authUtil = authUtil;
    }

    // GET /usuarios/me — retorna dados do usuário logado
    @GetMapping("/me")
    public ResponseEntity<UsuarioMeDTO> me(Authentication auth) {
        Usuario usuario = usuarioService.buscarPorId(authUtil.getUsuarioId(auth));
        return ResponseEntity.ok(new UsuarioMeDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole()
        ));
    }
}
