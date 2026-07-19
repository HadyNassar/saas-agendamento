package com.hady.saas.application.controller;

import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Endpoint TEMPORÁRIO para criar o primeiro usuário ADMIN_MASTER.
//
// ⚠️ IMPORTANTE: depois de criar seu usuário, REMOVA este controller
// inteiro e faça um novo deploy. Ele não tem proteção de autenticação
// (não poderia ter — é o que cria o primeiro admin) e não deve
// permanecer acessível em produção.
//
// Uso único via curl:
// curl -X POST https://api.oagendara.com.br/setup/criar-admin-master \
//   -H "Content-Type: application/json" \
//   -d '{"nome":"Seu Nome","email":"seuemail@exemplo.com","senha":"SuaSenhaForte123"}'
@RestController
@RequestMapping("/setup")
public class SetupTemporarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public SetupTemporarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/criar-admin-master")
    public ResponseEntity<Map<String, String>> criarAdminMaster(@RequestBody Map<String, String> body) {
        String nome = body.get("nome");
        String email = body.get("email");
        String senha = body.get("senha");

        if (nome == null || email == null || senha == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "nome, email e senha são obrigatórios"));
        }

        if (usuarioRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Este email já está cadastrado"));
        }

        Usuario admin = new Usuario();
        admin.setNome(nome);
        admin.setEmail(email);
        admin.setSenha(passwordEncoder.encode(senha));
        admin.setRole(Role.ADMIN_MASTER);
        admin.setEstabelecimento(null);

        usuarioRepository.save(admin);

        return ResponseEntity.ok(Map.of(
                "mensagem", "Admin master criado com sucesso! REMOVA ESTE CONTROLLER AGORA.",
                "email", email
        ));
    }
}