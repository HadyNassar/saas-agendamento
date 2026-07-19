package com.hady.saas.business.service;

import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.usuario.CodigoVerificacao2FA;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.CodigoVerificacao2FARepository;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import com.hady.saas.infrastructure.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthMasterService {

    private static final int CODIGO_VALIDO_MINUTOS = 5;

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final CodigoVerificacao2FARepository codigoRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final SecureRandom random = new SecureRandom();

    public AuthMasterService(AuthenticationManager authenticationManager,
                             UsuarioRepository usuarioRepository,
                             CodigoVerificacao2FARepository codigoRepository,
                             EmailService emailService,
                             JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.codigoRepository = codigoRepository;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
    }

    // Passo 1 — valida email/senha e, se corretos, envia o código 2FA.
    // Retorna o ID do usuário (necessário no passo 2 pra localizar o código
    // certo — não usamos email de novo pra evitar reenumeração de contas).
    @Transactional
    public Long iniciarLogin(String email, String senha) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, senha)
        );

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (usuario.getRole() != Role.ADMIN_MASTER) {
            throw new RuntimeException("Acesso restrito ao administrador do sistema.");
        }

        String codigo = gerarCodigoNumerico();
        CodigoVerificacao2FA registro = new CodigoVerificacao2FA(
                usuario.getId(), codigo, LocalDateTime.now().plusMinutes(CODIGO_VALIDO_MINUTOS)
        );
        codigoRepository.save(registro);

        emailService.enviarCodigo2FA(usuario.getEmail(), codigo);

        return usuario.getId();
    }

    // Passo 2 — confirma o código e emite o JWT definitivo do admin master.
    @Transactional
    public String confirmarCodigo(Long usuarioId, String codigo) {
        CodigoVerificacao2FA registro = codigoRepository
                .buscarCodigoValido(usuarioId, codigo, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Código inválido ou expirado."));

        registro.setUsado(true);
        codigoRepository.save(registro);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return jwtUtil.gerarTokenAdminMaster(usuario.getEmail(), usuario.getId());
    }

    private String gerarCodigoNumerico() {
        int numero = 100000 + random.nextInt(900000); // sempre 6 dígitos, 100000–999999
        return String.valueOf(numero);
    }
}