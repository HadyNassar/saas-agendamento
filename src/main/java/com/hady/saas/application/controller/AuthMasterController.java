package com.hady.saas.application.controller;

import com.hady.saas.application.dto.ConfirmarCodigo2FaDTO;
import com.hady.saas.application.dto.ConfirmarCodigo2FaDTO;
import com.hady.saas.application.dto.LoginMasterDTO;
import com.hady.saas.business.service.AuthMasterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Login exclusivo do ADMIN_MASTER (dono do SaaS), com 2FA por email.
// Fluxo em dois passos:
//   1. POST /auth-master/login       → valida email/senha, envia código
//   2. POST /auth-master/confirmar   → valida código, retorna JWT definitivo
@RestController
@RequestMapping("/auth-master")
public class AuthMasterController {

    private final AuthMasterService authMasterService;

    public AuthMasterController(AuthMasterService authMasterService) {
        this.authMasterService = authMasterService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginMasterDTO dto) {
        Long usuarioId = authMasterService.iniciarLogin(dto.getEmail(), dto.getSenha());
        return ResponseEntity.ok(Map.of(
                "usuarioId", usuarioId,
                "mensagem", "Código enviado para o seu email."
        ));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<Map<String, String>> confirmar(@Valid @RequestBody ConfirmarCodigo2FaDTO dto) {
        String token = authMasterService.confirmarCodigo(dto.getUsuarioId(), dto.getCodigo());
        return ResponseEntity.ok(Map.of("token", token));
    }
}