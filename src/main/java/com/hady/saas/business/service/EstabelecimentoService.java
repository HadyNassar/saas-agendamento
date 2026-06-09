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

    public EstabelecimentoService(EstabelecimentoRepository estabelecimentoRepository,
                                  UsuarioService usuarioService) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.usuarioService = usuarioService;
    }

    // ── Registro público: estabelecimento + ADMIN em uma transação atômica ───
    // O trial é iniciado aqui automaticamente — não depende de chamada extra
    // do frontend. Funciona independente de como a conta foi criada
    // (formulário, API, Postman, integração externa, etc).
    @Transactional
    public Estabelecimento registrar(RegistroDTO dto) {

        // 1. Monta o estabelecimento com plano STANDARD e trial já iniciado
        Estabelecimento estab = new Estabelecimento();
        estab.setNome(dto.getNomeEstabelecimento());
        estab.setTipo(dto.getTipoEstabelecimento());
        estab.setPlano(Plano.STANDARD);
        estab.setTelefone(dto.getTelefone());
        estab.setEndereco(dto.getEndereco());
        estab.iniciarTrial();
        Estabelecimento salvo = salvar(estab);

        // 2. Monta e salva o usuário ADMIN vinculado
        Usuario admin = new Usuario();
        admin.setNome(dto.getNome());
        admin.setEmail(dto.getEmail());
        admin.setSenha(dto.getSenha());   // UsuarioService criptografa
        admin.setRole(Role.ADMIN);
        admin.setEstabelecimento(salvo);
        usuarioService.salvarAdmin(admin);

        return salvo;
    }

    // ── Salva estabelecimento garantindo slug único ───────────────────────────
    public Estabelecimento salvar(Estabelecimento estabelecimento) {
        String slug = gerarSlug(estabelecimento.getNome());

        String slugFinal = slug;
        int contador = 1;
        while (estabelecimentoRepository.existsBySlug(slugFinal)) {
            slugFinal = slug + "-" + contador++;
        }

        estabelecimento.setSlug(slugFinal);
        return estabelecimentoRepository.save(estabelecimento);
    }

    // "Barbearia do João" → "barbearia-do-joao"
    private String gerarSlug(String nome) {
        return Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
