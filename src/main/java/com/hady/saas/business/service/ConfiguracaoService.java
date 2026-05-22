package com.hady.saas.business.service;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ConfiguracaoService {

    private final UsuarioRepository usuarioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final PasswordEncoder passwordEncoder;

    public ConfiguracaoService(UsuarioRepository usuarioRepository,
                               EstabelecimentoRepository estabelecimentoRepository,
                               PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Altera email do usuário logado
    public void alterarEmail(Long usuarioId, String novoEmail, String senhaAtual) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Valida a senha atual antes de alterar
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new RuntimeException("Senha atual incorreta");
        }

        // Verifica se o novo email já está em uso
        if (usuarioRepository.existsByEmail(novoEmail)) {
            throw new RuntimeException("Este email já está em uso");
        }

        usuario.setEmail(novoEmail);
        usuarioRepository.save(usuario);
    }

    // Altera senha do usuário logado
    public void alterarSenha(Long usuarioId, String senhaAtual, String novaSenha, String confirmarNovaSenha) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Valida a senha atual
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new RuntimeException("Senha atual incorreta");
        }

        // Valida se as novas senhas conferem
        if (!novaSenha.equals(confirmarNovaSenha)) {
            throw new RuntimeException("A nova senha e a confirmação não conferem");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    // Edita dados do estabelecimento — apenas ADMIN
    public Estabelecimento editarEstabelecimento(Long estabelecimentoId, String nome,
                                                  String telefone, String endereco) {

        Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        estabelecimento.setNome(nome);
        estabelecimento.setTelefone(telefone);
        estabelecimento.setEndereco(endereco);

        return estabelecimentoRepository.save(estabelecimento);
    }

    // Busca dados do estabelecimento para exibir na tela de configurações
    public Estabelecimento buscarEstabelecimento(Long estabelecimentoId) {
        return estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }
}
