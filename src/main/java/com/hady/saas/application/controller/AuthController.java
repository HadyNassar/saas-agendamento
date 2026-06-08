package com.hady.saas.application.controller;

import com.hady.saas.application.dto.LoginDTO;
import com.hady.saas.application.dto.LoginResponseDTO;
import com.hady.saas.application.dto.RegistroDTO;
import com.hady.saas.business.service.EstabelecimentoService;
import com.hady.saas.business.service.UsuarioService;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO dto) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getSenha())
        );

        Usuario usuario = usuarioService.buscarPorEmail(dto.getEmail());
        Estabelecimento estab = usuario.getEstabelecimento();
        String token = jwtUtil.generateToken(dto.getEmail(), usuario.getId(), estab.getId());

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole().name(),
                estab.getId(),
                estab.getNome(),
                estab.getSlug(),
                estab.getPlanoEfetivo() != null ? estab.getPlanoEfetivo().name() : null,
                estab.temAcesso(),
                estab.isTrialAtivo(),
                estab.getTrialFim() != null ? estab.getTrialFim().toString() : null
        );

        return ResponseEntity.ok(response);
    }

    // POST /auth/registro — cria o estabelecimento + usuário ADMIN atomicamente.
    // Toda a lógica (incluindo @Transactional) vive em EstabelecimentoService.registrar().
    @PostMapping("/registro")
    public ResponseEntity<String> registro(@Valid @RequestBody RegistroDTO dto) {
        Estabelecimento salvo = estabelecimentoService.registrar(dto);
        return ResponseEntity.ok("Estabelecimento criado com sucesso! Plano: " + salvo.getPlano().name());
    }
}
