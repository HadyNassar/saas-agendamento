package com.hady.saas.application.controller;

import com.hady.saas.application.dto.EstabelecimentoMapaDTO;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoints PÚBLICOS do mapa — sem autenticação, consumidos pelo
// frontend em encontre.oagendara.com.br
@RestController
@RequestMapping("/api/public")
public class MapaPublicoController {

    private final EstabelecimentoRepository estabelecimentoRepository;

    public MapaPublicoController(EstabelecimentoRepository estabelecimentoRepository) {
        this.estabelecimentoRepository = estabelecimentoRepository;
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
                        e.temAcesso() // só mostra "agendar online" se o plano estiver ativo/trial
                ))
                .toList();
    }
}