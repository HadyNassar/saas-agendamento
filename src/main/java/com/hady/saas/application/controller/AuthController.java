package com.hady.saas.application.controller;

import com.hady.saas.application.dto.LoginDTO;
import com.hady.saas.application.dto.RegistroDTO;
import com.hady.saas.business.service.EstabelecimentoService;
import com.hady.saas.business.service.UsuarioService;
import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;
    private final EstabelecimentoService estabelecimentoService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UsuarioService usuarioService,
                          EstabelecimentoService estabelecimentoService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioService = usuarioService;
        this.estabelecimentoService = estabelecimentoService;
    }

    // POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO dto) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getSenha())
        );

        String token = jwtUtil.generateToken(dto.getEmail());
        return ResponseEntity.ok(token);
    }

    // POST /auth/registro — cria o estabelecimento + usuário ADMIN de uma vez
    @PostMapping("/registro")
    public ResponseEntity<String> registro(@Valid @RequestBody RegistroDTO dto) {

        // 1. Cria o estabelecimento
        Estabelecimento estabelecimento = new Estabelecimento();
        estabelecimento.setNome(dto.getNomeEstabelecimento());
        estabelecimento.setTipo(dto.getTipoEstabelecimento());
        estabelecimento.setPlano(dto.getPlano());
        estabelecimento.setTelefone(dto.getTelefone());
        estabelecimento.setEndereco(dto.getEndereco());
        Estabelecimento salvo = estabelecimentoService.salvar(estabelecimento);

        // 2. Cria o usuário ADMIN vinculado ao estabelecimento
        Usuario admin = new Usuario();
        admin.setNome(dto.getNome());
        admin.setEmail(dto.getEmail());
        admin.setSenha(dto.getSenha()); // UsuarioService criptografa
        admin.setRole(Role.ADMIN);
        admin.setEstabelecimento(salvo);
        usuarioService.salvarAdmin(admin);

        return ResponseEntity.ok("Estabelecimento criado com sucesso! Plano: " + salvo.getPlano().name());
    }
}