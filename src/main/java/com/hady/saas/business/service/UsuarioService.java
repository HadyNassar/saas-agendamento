package com.hady.saas.business.service;

import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Usado no registro inicial — cria o ADMIN do estabelecimento
    // O ADMIN não conta no limite de usuários do plano
    public Usuario salvarAdmin(Usuario usuario) {
        validarEmailUnico(usuario.getEmail());
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }

    // Usado pelo ADMIN para cadastrar funcionários
    // Valida o limite do plano antes de salvar
    public Usuario salvarFuncionario(Usuario usuario) {
        Estabelecimento estabelecimento = usuario.getEstabelecimento();

        // Conta quantos funcionários o estabelecimento já tem (excluindo o ADMIN)
        int totalFuncionarios = usuarioRepository
                .countByEstabelecimentoIdAndRoleNot(estabelecimento.getId(), Role.ADMIN);

        if (!estabelecimento.podeCadastrarUsuario(totalFuncionarios)) {
            throw new RuntimeException(
                "Limite de usuários do plano " + estabelecimento.getPlano().name() +
                " atingido (" + estabelecimento.getPlano().getLimiteUsuarios() + " funcionários)"
            );
        }

        validarEmailUnico(usuario.getEmail());
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    private void validarEmailUnico(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("Email já cadastrado: " + email);
        }
    }
}
