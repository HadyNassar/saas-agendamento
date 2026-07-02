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
    private final EstabelecimentoService estabelecimentoService;

    public ConfiguracaoService(EstabelecimentoRepository estabelecimentoRepository,
                               UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder,
                               EstabelecimentoService estabelecimentoService) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.estabelecimentoService = estabelecimentoService;
    }

    @Transactional(readOnly = true)
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

        // CORREÇÃO: nome também protegido por null check, igual aos outros campos.
        // Assim o PATCH /detalhes pode passar null sem apagar o nome existente.
        //
        // Link público vinculado ao nome: sempre que o nome realmente mudar
        // (ignorando espaços nas pontas e diferença de maiúscula/minúscula),
        // o slug é recalculado a partir do novo nome, garantindo unicidade
        // (sufixo -1, -2... se já existir outro estabelecimento com o mesmo
        // nome). Não há endpoint separado para editar o slug — ele é sempre
        // derivado do nome automaticamente.
        if (nome != null && !nome.isBlank() && !nome.trim().equalsIgnoreCase(est.getNome())) {
            est.setNome(nome.trim());
            est.setSlug(estabelecimentoService.gerarSlugUnico(nome.trim(), est.getId()));
        }
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
    public void atualizarTipo(Long estabelecimentoId, String tipo) {
        Estabelecimento est = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        if (tipo == null || tipo.isBlank()) {
            throw new RuntimeException("Informe o tipo de estabelecimento.");
        }
        try {
            est.setTipo(com.hady.saas.domain.enums.TipoEstabelecimento.valueOf(tipo));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Tipo de estabelecimento inválido.");
        }

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