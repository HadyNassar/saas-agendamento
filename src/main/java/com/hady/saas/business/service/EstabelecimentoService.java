package com.hady.saas.business.service;

import com.hady.saas.application.dto.RegistroDTO;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    // ── Registro público: estabelecimento + ADMIN em uma transação atômica ───
    @Transactional
    public Estabelecimento registrar(RegistroDTO dto) {

        Estabelecimento estab = new Estabelecimento();
        estab.setNome(dto.getNomeEstabelecimento());
        estab.setTipo(dto.getTipoEstabelecimento());
        estab.setPlano(Plano.STANDARD);
        estab.setTelefone(dto.getTelefone());

        // Endereço estruturado (vindo do CEP preenchido automaticamente no front)
        estab.setCep(dto.getCep());
        estab.setLogradouro(dto.getLogradouro());
        estab.setNumero(dto.getNumero());
        estab.setComplemento(dto.getComplemento());
        estab.setBairro(dto.getBairro());
        estab.setCidade(dto.getCidade());
        estab.setEstado(dto.getEstado());

        // Mantém o campo de texto livre preenchido também, por compatibilidade
        // com telas antigas que ainda leem 'endereco'.
        estab.setEndereco(estab.getEnderecoCompleto());

        estab.iniciarTrial();

        // Geocodifica usando o endereço completo estruturado — muito mais
        // confiável que texto livre, pois sempre tem cidade e estado.
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

    public Estabelecimento salvar(Estabelecimento estabelecimento) {
        estabelecimento.setSlug(gerarSlugUnico(estabelecimento.getNome(), estabelecimento.getId()));
        return estabelecimentoRepository.save(estabelecimento);
    }

    // Geocodifica o endereço e salva lat/lng. Silencioso em caso de falha.
    private void geocodificarSePossivel(Estabelecimento estab) {
        String enderecoCompleto = estab.getEnderecoCompleto();
        if (enderecoCompleto == null || enderecoCompleto.isBlank()) return;
        try {
            double[] coords = geocodingService.geocodificarEndereco(enderecoCompleto);
            if (coords != null) {
                estab.setLatitude(BigDecimal.valueOf(coords[0]));
                estab.setLongitude(BigDecimal.valueOf(coords[1]));
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