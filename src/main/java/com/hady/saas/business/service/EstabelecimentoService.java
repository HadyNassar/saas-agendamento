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

    // ── Salva estabelecimento NOVO garantindo slug único ──────────────────────
    // Usado apenas no fluxo de registro (id ainda é null aqui).
    public Estabelecimento salvar(Estabelecimento estabelecimento) {
        estabelecimento.setSlug(gerarSlugUnico(estabelecimento.getNome(), estabelecimento.getId()));
        return estabelecimentoRepository.save(estabelecimento);
    }

    // ── Regenera o slug de um estabelecimento JÁ EXISTENTE a partir do nome
    // atual, mantendo unicidade. Chamado sempre que o nome muda (link público
    // fica automaticamente vinculado ao nome, sem precisar de tela própria
    // para editar o link). Se o slug calculado colidir, ganha sufixo -1, -2...
    public String gerarSlugUnico(String nome, Long idEstabelecimentoAtual) {
        String base = gerarSlug(nome);
        if (base.isBlank()) base = "estabelecimento"; // fallback de segurança, nunca deixa slug vazio

        String slugFinal = base;
        int contador = 1;
        while (slugColide(slugFinal, idEstabelecimentoAtual)) {
            slugFinal = base + "-" + contador++;
        }
        return slugFinal;
    }

    // Checa colisão de slug ignorando o próprio registro (relevante em edição;
    // em criação, idEstabelecimentoAtual é null e cai na checagem simples).
    private boolean slugColide(String slug, Long idEstabelecimentoAtual) {
        if (idEstabelecimentoAtual == null) {
            return estabelecimentoRepository.existsBySlug(slug);
        }
        return estabelecimentoRepository.existsBySlugAndIdNot(slug, idEstabelecimentoAtual);
    }

    // "Barbearia do João" → "barbearia-do-joao"
    private String gerarSlug(String nome) {
        if (nome == null) return "";
        return Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}