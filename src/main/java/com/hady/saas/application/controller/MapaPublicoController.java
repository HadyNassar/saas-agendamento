package com.hady.saas.application.controller;

import com.hady.saas.application.dto.EstabelecimentoMapaDTO;
import com.hady.saas.application.dto.EstabelecimentoPerfilPublicoDTO;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

// Endpoints PÚBLICOS do mapa — sem autenticação, consumidos pelo
// frontend em encontre.oagendara.com.br
@RestController
@RequestMapping("/api/public")
public class MapaPublicoController {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final JdbcTemplate jdbcTemplate;

    public MapaPublicoController(EstabelecimentoRepository estabelecimentoRepository,
                                 JdbcTemplate jdbcTemplate) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // Lista estabelecimentos cadastrados (pins verdes) próximos a um ponto.
    // GET /api/public/estabelecimentos?lat=-20.55&lng=-54.62&raio=10
    @GetMapping("/estabelecimentos")
    public List<EstabelecimentoMapaDTO> listarProximos(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double raio) {

        List<Estabelecimento> proximos = estabelecimentoRepository.buscarProximos(lat, lng, raio);

        return proximos.stream()
                .map(e -> new EstabelecimentoMapaDTO(
                        e.getId(),
                        e.getNome(),
                        e.getSlug(),
                        e.getTipo() != null ? e.getTipo().name() : null,
                        e.getBairro(),
                        e.getCidade(),
                        e.getLatitude(),
                        e.getLongitude(),
                        e.getFotoPerfil(),
                        e.temAcesso()
                ))
                .toList();
    }

    // Perfil público completo de um salão — aberto ao clicar no pin verde.
    // GET /api/public/estabelecimentos/{slug}
    // Também registra a visualização (usada depois como argumento comercial:
    // "seu salão teve X visualizações esse mês").
    @GetMapping("/estabelecimentos/{slug}")
    @Transactional(readOnly = true)
    public EstabelecimentoPerfilPublicoDTO buscarPerfil(@PathVariable String slug,
                                                        @RequestParam(required = false, defaultValue = "mapa") String origem) {
        Estabelecimento e = estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        EstabelecimentoPerfilPublicoDTO dto = new EstabelecimentoPerfilPublicoDTO(
                e.getId(),
                e.getNome(),
                e.getSlug(),
                e.getTipo() != null ? e.getTipo().name() : null,
                e.getTelefone(),
                e.getWhatsappContato(),
                e.getInstagram(),
                e.getFacebook(),
                e.getFotoPerfil(),
                e.getLogradouro(),
                e.getNumero(),
                e.getBairro(),
                e.getCidade(),
                e.getEstado(),
                e.getLatitude(),
                e.getLongitude(),
                new ArrayList<>(e.getComodidades()),
                new ArrayList<>(e.getFormasPagamento()),
                e.temAcesso()
        );

        // Registrado FORA da transação de leitura (readOnly) — método próprio
        // com @Transactional(propagation = REQUIRES_NEW), roda em transação
        // separada e nunca invalida a consulta acima mesmo se falhar.
        registrarVisualizacao(e.getId(), origem);

        return dto;
    }

    // Grava um registro simples de visualização em transação PRÓPRIA,
    // separada da consulta principal (readOnly). Assim, mesmo se o insert
    // falhar por qualquer motivo, nunca compromete a resposta do perfil.
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void registrarVisualizacao(Long estabelecimentoId, String origem) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO mapa_visualizacoes (salao_id, origem) VALUES (?, ?)",
                    estabelecimentoId, origem
            );
        } catch (Exception e) {
            System.err.println("Falha ao registrar visualização: " + e.getMessage());
        }
    }
}