package com.hady.saas.application.controller;

import com.hady.saas.application.dto.FotoProfissionalDTO;
import com.hady.saas.business.service.FotoService;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.FotoProfissionalRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints públicos (sem autenticação) para a página de agendamento.
 *
 * Portfólio do profissional:
 *   GET /app/{slug}/profissionais/{profissionalId}/fotos
 *   → retorna lista de DTOs com URLs pré-assinadas do R2 (válidas por 1h)
 *
 * REMOVIDO: GET /app/{slug}/clientes/{clienteId}/ficha
 * A ficha do cliente é privada — somente via painel autenticado.
 */
@RestController
@RequestMapping("/app/{slug}")
public class FotoPublicoController {

    private final FotoService fotoService;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final FotoProfissionalRepository fotoProfissionalRepository;

    public FotoPublicoController(FotoService fotoService,
                                 EstabelecimentoRepository estabelecimentoRepository,
                                 FotoProfissionalRepository fotoProfissionalRepository) {
        this.fotoService = fotoService;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.fotoProfissionalRepository = fotoProfissionalRepository;
    }

    /**
     * Portfólio de fotos do profissional — exibido antes do cliente agendar.
     *
     * Valida que o profissional pertence ao estabelecimento do slug (isolamento multi-tenant).
     * Retorna URLs pré-assinadas do R2 (válidas 1h) — sem expor credenciais nem path interno.
     */
    @GetMapping("/profissionais/{profissionalId}/fotos")
    @Transactional(readOnly = true)
    public ResponseEntity<List<FotoProfissionalDTO>> portfolioProfissional(
            @PathVariable String slug,
            @PathVariable Long profissionalId) {

        Estabelecimento est = buscarEstabelecimento(slug);
        Long estabelecimentoId = est.getId();

        // Busca as fotos do profissional validando isolamento por estabelecimento
        List<FotoProfissionalDTO> fotos = fotoProfissionalRepository
                .findByProfissionalIdOrderByCriadoEmDesc(profissionalId)
                .stream()
                .filter(f -> f.getProfissional().getEstabelecimento().getId().equals(estabelecimentoId))
                .map(f -> {
                    // Gera URL pré-assinada (1h) para cada foto — acessível sem JWT
                    String urlPresignada = fotoService.gerarUrlPublicaProfissional(f.getId(), estabelecimentoId);
                    FotoProfissionalDTO dto = new FotoProfissionalDTO();
                    dto.setId(f.getId());
                    dto.setUrl(urlPresignada);
                    dto.setCriadoEm(f.getCriadoEm());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(fotos);
    }

    private Estabelecimento buscarEstabelecimento(String slug) {
        return estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }
}