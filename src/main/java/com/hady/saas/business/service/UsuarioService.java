package com.hady.saas.business.service;

import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    // Limite de ADMINs por estabelecimento.
    // O registro inicial cria 1 ADMIN. O ADMIN pode criar mais 1, totalizando 2.
    private static final int LIMITE_ADMINS = 2;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ─── Registro inicial: cria o primeiro ADMIN do estabelecimento ───────────
    // Chamado apenas em EstabelecimentoService.registrar() — não valida limite pois
    // é o primeiro usuário (sempre haverá exatamente 1 ADMIN ao registrar).
    public Usuario salvarAdmin(Usuario usuario) {
        validarEmailUnico(usuario.getEmail());
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }

    // ─── Cadastro de funcionário via painel (qualquer role exceto o primeiro ADMIN) ──
    // Valida:
    //   1. Limite de ADMINs (máx. 2 por estabelecimento).
    //   2. Limite total de usuários do plano (exclui ADMINs da contagem).
    //   3. E-mail único global.
    public Usuario salvarFuncionario(Usuario usuario) {
        Estabelecimento estabelecimento = usuario.getEstabelecimento();
        Role role = usuario.getRole();
        Plano planoEfetivo = estabelecimento.getPlanoEfetivo();

        // ── 1. Limite de ADMINs (máx. 2) ──────────────────────────────────
        if (role == Role.ADMIN) {
            int totalAdmins = usuarioRepository
                    .countByEstabelecimentoIdAndRole(estabelecimento.getId(), Role.ADMIN);
            if (totalAdmins >= LIMITE_ADMINS) {
                throw new RuntimeException(
                    "Limite de " + LIMITE_ADMINS + " ADMINs por estabelecimento atingido.");
            }
        }

        // ── 2. Limite total de usuários do plano (ADMINs não contam) ──────
        if (role != Role.ADMIN) {
            int totalFuncionarios = usuarioRepository
                    .countByEstabelecimentoIdAndRoleNot(estabelecimento.getId(), Role.ADMIN);
            if (!estabelecimento.podeCadastrarUsuario(totalFuncionarios)) {
                throw new RuntimeException(
                    "Limite de usuários do plano " + planoEfetivo.name() +
                    " atingido (" + planoEfetivo.getLimiteUsuarios() + " funcionários)");
            }
        }

        validarEmailUnico(usuario.getEmail());
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmailComEstabelecimento(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    private void validarEmailUnico(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("Email já cadastrado: " + email);
        }
    }
}
