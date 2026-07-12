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
    private final GeocodingService geocodingService;

    public ConfiguracaoService(EstabelecimentoRepository estabelecimentoRepository,
                               UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder,
                               EstabelecimentoService estabelecimentoService,
                               GeocodingService geocodingService) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.estabelecimentoService = estabelecimentoService;
        this.geocodingService = geocodingService;
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

        if (nome != null && !nome.isBlank() && !nome.trim().equalsIgnoreCase(est.getNome())) {
            est.setNome(nome.trim());
            est.setSlug(estabelecimentoService.gerarSlugUnico(nome.trim(), est.getId()));
        }

        // Se o endereço mudou, regeocodifica automaticamente
        if (endereco != null && !endereco.equals(est.getEndereco())) {
            est.setEndereco(endereco);
            geocodificarSePossivel(est);
        }

        if (telefone != null)          est.setTelefone(telefone);
        if (mesesAntecipacao != null)  est.setMesesAntecipacao(mesesAntecipacao);
        if (fotoPerfil != null)        est.setFotoPerfil(fotoPerfil);
        if (instagram != null)         est.setInstagram(instagram);
        if (whatsappContato != null)   est.setWhatsappContato(whatsappContato);
        if (facebook != null)          est.setFacebook(facebook);
        if (comodidades != null)       est.setComodidades(comodidades);
        if (formasPagamento != null)   est.setFormasPagamento(formasPagamento);

        estabelecimentoRepository.save(est);
    }

    // Popula coordenadas de todos os estabelecimentos que ainda não têm lat/lng
    // Chamado pelo endpoint admin /admin/geocodificar-todos
    @Transactional
    public int geocodificarTodos() {
        List<Estabelecimento> semCoordenadas = estabelecimentoRepository
                .findAll()
                .stream()
                .filter(e -> e.getLatitude() == null && e.getEndereco() != null && !e.getEndereco().isBlank())
                .toList();

        int atualizados = 0;
        for (Estabelecimento est : semCoordenadas) {
            try {
                double[] coords = geocodingService.geocodificarEndereco(est.getEndereco());
                if (coords != null) {
                    est.setLatitude(BigDecimal.valueOf(coords[0]));
                    est.setLongitude(BigDecimal.valueOf(coords[1]));
                    estabelecimentoRepository.save(est);
                    atualizados++;
                }
                // Pausa para não estourar o rate limit da API
                Thread.sleep(200);
            } catch (Exception e) {
                System.err.println("Erro ao geocodificar " + est.getNome() + ": " + e.getMessage());
            }
        }
        return atualizados;
    }

    private void geocodificarSePossivel(Estabelecimento est) {
        if (est.getEndereco() == null || est.getEndereco().isBlank()) return;
        try {
            double[] coords = geocodingService.geocodificarEndereco(est.getEndereco());
            if (coords != null) {
                est.setLatitude(BigDecimal.valueOf(coords[0]));
                est.setLongitude(BigDecimal.valueOf(coords[1]));
            }
        } catch (Exception e) {
            System.err.println("Geocodificação falhou silenciosamente: " + e.getMessage());
        }
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