package com.hady.saas.application.controller;

import com.hady.saas.business.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints públicos (sem JWT) para recuperação de senha.
 *
 *  POST /auth/esqueci-senha          → recebe o e-mail, envia o link
 *  GET  /auth/validar-token?token=X  → verifica se o token ainda é válido
 *  POST /auth/redefinir-senha        → troca a senha com o token
 */
@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    // POST /auth/esqueci-senha
    // Body: { "email": "usuario@email.com" }
    // Sempre retorna 200 — não revela se o e-mail existe (prevenção de enumeração)
    @PostMapping("/esqueci-senha")
    public ResponseEntity<Map<String, String>> esqueceuSenha(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensagem", "Informe o e-mail."));
        }
        passwordResetService.solicitarReset(email.trim().toLowerCase());
        return ResponseEntity.ok(Map.of(
                "mensagem", "Se este e-mail estiver cadastrado, você receberá as instruções em breve."
        ));
    }

    // GET /auth/validar-token?token=UUID
    // Usado pelo frontend para verificar se o token ainda é válido antes de exibir o formulário
    @GetMapping("/validar-token")
    public ResponseEntity<Map<String, String>> validarToken(@RequestParam String token) {
        passwordResetService.validarToken(token);
        return ResponseEntity.ok(Map.of("mensagem", "Token válido."));
    }

    // POST /auth/redefinir-senha
    // Body: { "token": "UUID", "novaSenha": "...", "confirmarSenha": "..." }
    @PostMapping("/redefinir-senha")
    public ResponseEntity<Map<String, String>> redefinirSenha(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String novaSenha = body.get("novaSenha");
        String confirmarSenha = body.get("confirmarSenha");

        if (token == null || novaSenha == null || confirmarSenha == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensagem", "Token, nova senha e confirmação são obrigatórios."));
        }

        passwordResetService.confirmarReset(token.trim(), novaSenha, confirmarSenha);
        return ResponseEntity.ok(Map.of(
                "mensagem", "Senha redefinida com sucesso! Faça login com sua nova senha."
        ));
    }
}