package com.hady.saas.business.service;

import com.hady.saas.application.dto.RegistroDTO;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;

@Service
public class EstabelecimentoService {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final UsuarioService usuarioService;
    private final GeocodingService geocodingService;

    public EstabelecimentoService(EstabelecimentoRepository estabelecimentoRepository,
                                  UsuarioService usuarioService,
                                  GeocodingService geocodingService) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.usuarioService = usuarioService;
        this.geocodingService = geocodingService;
    }

    @Transactional
    public Estabelecimento registrar(RegistroDTO dto) {

        Estabelecimento estab = new Estabelecimento();
        estab.setNome(dto.getNomeEstabelecimento());
        estab.setTipo(dto.getTipoEstabelecimento());
        estab.setPlano(Plano.STANDARD);
        estab.setTelefone(dto.getTelefone());
        estab.setEndereco(dto.getEndereco());
        estab.iniciarTrial();

        // Geocodifica o endereço automaticamente ao registrar
        geocodificarSePossivel(estab);

        Estabelecimento salvo = salvar(estab);

        Usuario admin = new Usuario();
        admin.setNome(dto.getNome());
        admin.setEmail(dto.getEmail());
        admin.setSenha(dto.getSenha());
        admin.setRole(Role.ADMIN);
        admin.setEstabelecimento(salvo);
        usuarioService.salvarAdmin(admin);

        return salvo;
    }

    // Chamado ao atualizar dados do estabelecimento (endereço pode ter mudado)
    @Transactional
    public Estabelecimento atualizar(Estabelecimento estabelecimento, String novoEndereco) {
        boolean enderecoMudou = novoEndereco != null &&
                !novoEndereco.equals(estabelecimento.getEndereco());

        estabelecimento.setEndereco(novoEndereco);

        if (enderecoMudou) {
            geocodificarSePossivel(estabelecimento);
        }

        return salvar(estabelecimento);
    }

    public Estabelecimento salvar(Estabelecimento estabelecimento) {
        estabelecimento.setSlug(gerarSlugUnico(estabelecimento.getNome(), estabelecimento.getId()));
        return estabelecimentoRepository.save(estabelecimento);
    }

    // Geocodifica o endereço e salva lat/lng no estabelecimento
    // Se falhar (endereço inválido, API fora, etc) não interrompe o fluxo
    private void geocodificarSePossivel(Estabelecimento estab) {
        if (estab.getEndereco() == null || estab.getEndereco().isBlank()) return;
        try {
            double[] coords = geocodingService.geocodificarEndereco(estab.getEndereco());
            if (coords != null) {
                estab.setLatitude(java.math.BigDecimal.valueOf(coords[0]));
                estab.setLongitude(java.math.BigDecimal.valueOf(coords[1]));
            }
        } catch (Exception e) {
            System.err.println("Geocodificação falhou silenciosamente: " + e.getMessage());
        }
    }

    public String gerarSlugUnico(String nome, Long idEstabelecimentoAtual) {
        String base = gerarSlug(nome);
        if (base.isBlank()) base = "estabelecimento";

        String slugFinal = base;
        int contador = 1;
        while (slugColide(slugFinal, idEstabelecimentoAtual)) {
            slugFinal = base + "-" + contador++;
        }
        return slugFinal;
    }

    private boolean slugColide(String slug, Long idEstabelecimentoAtual) {
        if (idEstabelecimentoAtual == null) {
            return estabelecimentoRepository.existsBySlug(slug);
        }
        return estabelecimentoRepository.existsBySlugAndIdNot(slug, idEstabelecimentoAtual);
    }

    private String gerarSlug(String nome) {
        if (nome == null) return "";
        return Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}