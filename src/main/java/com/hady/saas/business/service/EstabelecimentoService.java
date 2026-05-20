package com.hady.saas.business.service;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;

@Service
@RequiredArgsConstructor
public class EstabelecimentoService {

    private final EstabelecimentoRepository estabelecimentoRepository;

    public Estabelecimento salvar(Estabelecimento estabelecimento) {
        String slug = gerarSlug(estabelecimento.getNome());

        // Garante slug único adicionando número se necessário
        String slugFinal = slug;
        int contador = 1;
        while (estabelecimentoRepository.existsBySlug(slugFinal)) {
            slugFinal = slug + "-" + contador++;
        }

        estabelecimento.setSlug(slugFinal);
        return estabelecimentoRepository.save(estabelecimento);
    }

    // Gera slug a partir do nome: "Barbearia do João" -> "barbearia-do-joao"
    private String gerarSlug(String nome) {
        return Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
