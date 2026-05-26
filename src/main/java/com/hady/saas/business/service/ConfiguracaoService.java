package com.hady.saas.business.service;

// ──────────────────────────────────────────────────────────────────────────────
// ATENÇÃO: este arquivo mostra APENAS o método editarEstabelecimento atualizado.
// Copie somente esse método para dentro do seu ConfiguracaoService existente,
// substituindo o método antigo.
// ──────────────────────────────────────────────────────────────────────────────

// No seu ConfiguracaoService, localize o método editarEstabelecimento e
// substitua pela versão abaixo (adiciona o parâmetro mesesAntecipacao):

/*
    public void editarEstabelecimento(Long estabelecimentoId,
                                      String nome,
                                      String telefone,
                                      String endereco,
                                      Integer mesesAntecipacao) {

        Estabelecimento est = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        est.setNome(nome);
        if (telefone != null) est.setTelefone(telefone);
        if (endereco != null) est.setEndereco(endereco);
        if (mesesAntecipacao != null) est.setMesesAntecipacao(mesesAntecipacao);

        estabelecimentoRepository.save(est);
    }
*/

// Se preferir, abaixo está o serviço completo como referência.
// Ajuste os imports e dependências conforme o seu projeto.

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import com.hady.saas.domain.usuario.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ConfiguracaoService {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ConfiguracaoService(EstabelecimentoRepository estabelecimentoRepository,
                                UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Estabelecimento buscarEstabelecimento(Long id) {
        return estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }

    // ✅ Método atualizado — aceita mesesAntecipacao
    public void editarEstabelecimento(Long estabelecimentoId,
                                      String nome,
                                      String telefone,
                                      String endereco,
                                      Integer mesesAntecipacao) {

        Estabelecimento est = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        est.setNome(nome);
        if (telefone != null) est.setTelefone(telefone);
        if (endereco != null) est.setEndereco(endereco);
        if (mesesAntecipacao != null) est.setMesesAntecipacao(mesesAntecipacao);

        estabelecimentoRepository.save(est);
    }

    public void alterarEmail(Long usuarioId, String novoEmail, String senhaAtual) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new RuntimeException("Senha atual incorreta");
        }
        if (usuarioRepository.existsByEmail(novoEmail)) {
            throw new RuntimeException("Este email já está em uso");
        }

        usuario.setEmail(novoEmail);
        usuarioRepository.save(usuario);
    }

    public void alterarSenha(Long usuarioId, String senhaAtual, String novaSenha, String confirmarNovaSenha) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new RuntimeException("Senha atual incorreta");
        }
        if (!novaSenha.equals(confirmarNovaSenha)) {
            throw new RuntimeException("As senhas não coincidem");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }
}
