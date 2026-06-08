package com.hady.saas.application.controller;

import com.hady.saas.application.dto.FotoProfissionalDTO;
import com.hady.saas.business.service.FotoService;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints públicos (sem autenticação) para a página de agendamento.
 *
 * Portfólio do profissional:
 *   GET /app/{slug}/profissionais/{profissionalId}/fotos
 *   → fotos do portfólio exibidas na escolha do profissional
 *
 * REMOVIDO: GET /app/{slug}/clientes/{clienteId}/ficha
 * A ficha do cliente (histórico, anamnese, fotos antes/depois) é privada
 * e só pode ser acessada pelo painel autenticado em /painel/clientes/{id}/ficha.
 */
@RestController
@RequestMapping("/app/{slug}")
public class FotoPublicoController {

    private final FotoService fotoService;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public FotoPublicoController(FotoService fotoService,
                                  EstabelecimentoRepository estabelecimentoRepository) {
        this.fotoService = fotoService;
        this.estabelecimentoRepository = estabelecimentoRepository;
    }

    /**
     * Portfólio de fotos do profissional — exibido antes do cliente agendar.
     * Valida que o profissional pertence ao estabelecimento do slug para
     * impedir enumeração de fotos de profissionais de outros tenants.
     */
    @GetMapping("/profissionais/{profissionalId}/fotos")
    public ResponseEntity<List<FotoProfissionalDTO>> portfolioProfissional(
            @PathVariable String slug,
            @PathVariable Long profissionalId) {

        Estabelecimento est = buscarEstabelecimento(slug);
        return ResponseEntity.ok(fotoService.listarFotosProfissional(profissionalId, est.getId()));
    }

    private Estabelecimento buscarEstabelecimento(String slug) {
        return estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }
}
