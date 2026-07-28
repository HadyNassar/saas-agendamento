package com.hady.saas.application.controller;

import com.hady.saas.business.service.IndicacaoService;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.indicacao.Indicacao;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Item 17 — indicação/referral automatizada. Endpoint público que o
 * indicado abre a partir do link recebido pelo indicador.
 *
 *   GET  /app/{slug}/indicar/{codigo}          → verifica se o link é válido
 *                                                 (para montar a tela antes
 *                                                 de pedir os dados)
 *   POST /app/{slug}/indicar/{codigo}          → identifica/cria o indicado
 *                                                 e vincula à indicação
 */
@RestController
@RequestMapping("/app/{slug}/indicar")
public class IndicacaoPublicoController {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final IndicacaoService indicacaoService;

    public IndicacaoPublicoController(EstabelecimentoRepository estabelecimentoRepository,
                                      IndicacaoService indicacaoService) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.indicacaoService = indicacaoService;
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<Map<String, Object>> verificar(@PathVariable String slug,
                                                         @PathVariable String codigo) {
        Estabelecimento estab = buscarEstabelecimento(slug);
        boolean valido = indicacaoService.linkValido(codigo, estab);

        if (!valido) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valido", false,
                    "erro", "Este link de indicação não existe, já foi usado ou expirou."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "valido", true,
                "estabelecimento", estab.getNome()
        ));
    }

    @PostMapping("/{codigo}")
    public ResponseEntity<Map<String, Object>> usar(@PathVariable String slug,
                                                    @PathVariable String codigo,
                                                    @Valid @RequestBody UsarIndicacaoDTO dto) {
        Estabelecimento estab = buscarEstabelecimento(slug);

        try {
            Indicacao indicacao = indicacaoService.usarLink(codigo, dto.getTelefone(), dto.getNome(), estab);
            return ResponseEntity.ok(Map.of(
                    "sucesso", true,
                    "mensagem", "Link de indicação confirmado! Agende seu primeiro horário para garantir seu cupom."
            ));
        } catch (IndicacaoService.IndicacaoInvalidaException e) {
            return ResponseEntity.badRequest().body(Map.of("sucesso", false, "erro", e.getMessage()));
        }
    }

    private Estabelecimento buscarEstabelecimento(String slug) {
        return estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }

    public static class UsarIndicacaoDTO {
        @NotBlank
        private String telefone;
        private String nome;

        public String getTelefone()          { return telefone; }
        public void setTelefone(String v)    { this.telefone = v; }
        public String getNome()              { return nome; }
        public void setNome(String v)        { this.nome = v; }
    }
}