package com.hady.saas.business.service;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import com.hady.saas.domain.usuario.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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

    @Transactional
    public void editarEstabelecimento(Long estabelecimentoId,
                                      String nome,
                                      String telefone,
                                      String endereco,
                                      Integer mesesAntecipacao,
                                      String fotoPerfil,
                                      String instagram,
                                      String whatsappContato,
                                      String facebook,
                                      List<String> comodidades,
                                      List<String> formasPagamento) {

        Estabelecimento est = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        est.setNome(nome);
        if (telefone != null)          est.setTelefone(telefone);
        if (endereco != null)          est.setEndereco(endereco);
        if (mesesAntecipacao != null)  est.setMesesAntecipacao(mesesAntecipacao);
        if (fotoPerfil != null)        est.setFotoPerfil(fotoPerfil);
        if (instagram != null)         est.setInstagram(instagram);
        if (whatsappContato != null)   est.setWhatsappContato(whatsappContato);
        if (facebook != null)          est.setFacebook(facebook);
        if (comodidades != null)       est.setComodidades(comodidades);
        if (formasPagamento != null)   est.setFormasPagamento(formasPagamento);

        estabelecimentoRepository.save(est);
    }

    @Transactional
    public void atualizarSinal(Long estabelecimentoId, Boolean exigirSinal, String tipoSinal, BigDecimal valorSinal) {
        Estabelecimento est = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        est.setExigirSinal(exigirSinal != null && exigirSinal);
        est.setTipoSinal(tipoSinal);
        est.setValorSinal(valorSinal);

        estabelecimentoRepository.save(est);
    }

    @Transactional
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

    @Transactional
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
        usuario.setSenhaAtualizadaEm(java.time.Instant.now());
        usuarioRepository.save(usuario);
    }
}