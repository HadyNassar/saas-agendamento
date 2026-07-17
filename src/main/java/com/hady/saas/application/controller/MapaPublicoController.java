package com.hady.saas.application.controller;

import com.hady.saas.application.dto.EstabelecimentoMapaDTO;
import com.hady.saas.application.dto.EstabelecimentoPerfilPublicoDTO;
import com.hady.saas.application.dto.RegistrarInteresseDTO;
import com.hady.saas.application.dto.SalaoExternoAdminDTO;
import com.hady.saas.application.dto.SalaoExternoDTO;
import com.hady.saas.business.service.GooglePlacesService;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Endpoints do mapa. A maioria é pública (sem autenticação), consumida
// pelo frontend em encontre.oagendara.com.br. Os endpoints de prospecção
// (avaliação e telefone de salão não cadastrado) exigem login de ADMIN —
// são a vantagem exclusiva de quem usa o sistema pra prospectar, e nunca
// ficam visíveis ao cliente final nem a um salão não pagante.
@RestController
public class MapaPublicoController {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final JdbcTemplate jdbcTemplate;
    private final GooglePlacesService googlePlacesService;

    public MapaPublicoController(EstabelecimentoRepository estabelecimentoRepository,
                                 JdbcTemplate jdbcTemplate,
                                 GooglePlacesService googlePlacesService) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.googlePlacesService = googlePlacesService;
    }

    // ═══════════════════════════════════════════════════════════════════
    // PÚBLICO — sem autenticação
    // ═══════════════════════════════════════════════════════════════════

    @GetMapping("/api/public/estabelecimentos")
    public List<EstabelecimentoMapaDTO> listarProximos(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double raio,
            @RequestParam(required = false) String tipo) {

        List<Estabelecimento> proximos = estabelecimentoRepository.buscarProximos(lat, lng, raio);

        return proximos.stream()
                .filter(e -> tipo == null || tipo.isBlank()
                        || (e.getTipo() != null && e.getTipo().name().equalsIgnoreCase(tipo)))
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

    // Busca salões da região via Google Places (pins cinza) para o CLIENTE
    // FINAL. Propositalmente SEM avaliação e SEM telefone — isso incentiva
    // o dono do salão a se cadastrar pra desbloquear esses dados pro público.
    @GetMapping("/api/public/estabelecimentos-externos")
    public List<SalaoExternoDTO> buscarExternosPublico(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double raio,
            @RequestParam(required = false) String categoria) {

        return filtrarNaoCadastrados(lat, lng, raio).stream()
                .filter(s -> categoria == null || categoria.isBlank()
                        || (s.getCategoriaBusca() != null && s.getCategoriaBusca().equalsIgnoreCase(categoria)))
                .map(s -> new SalaoExternoDTO(
                        s.getGooglePlaceId(), s.getNome(), s.getEndereco(),
                        s.getLatitude(), s.getLongitude(), s.getCategoriaBusca()
                ))
                .toList();
    }

    // Registra o interesse de um cliente final que tentou agendar em um
    // salão AINDA NÃO cadastrado (pin cinza). Vira munição comercial.
    @PostMapping("/api/public/interesse")
    public ResponseEntity<String> registrarInteresse(@Valid @RequestBody RegistrarInteresseDTO dto) {
        jdbcTemplate.update(
                "INSERT INTO mapa_interesses (nome_cliente, contato, google_place_id, nome_salao_externo) " +
                        "VALUES (?, ?, ?, ?)",
                dto.getNomeCliente(), dto.getContato(), dto.getGooglePlaceId(), dto.getNomeSalaoExterno()
        );
        return ResponseEntity.ok("Interesse registrado! Avisaremos assim que este salão estiver disponível.");
    }

    // Perfil público completo de um salão CADASTRADO — aberto ao clicar
    // no pin verde. Registra a visualização (argumento comercial futuro).
    @GetMapping("/api/public/estabelecimentos/{slug}")
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

        registrarVisualizacao(e.getId(), origem);

        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════
    // ADMIN — exige login (rota /admin/mapa/** cai em .anyRequest()
    // .authenticated() do SecurityConfig, então só logado acessa)
    // ═══════════════════════════════════════════════════════════════════

    // Mesma busca de pins cinza, mas com avaliação do Google incluída —
    // ferramenta de prospecção pra você avaliar quais salões abordar primeiro.
    @GetMapping("/admin/mapa/estabelecimentos-externos")
    public List<SalaoExternoAdminDTO> buscarExternosAdmin(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double raio) {

        return filtrarNaoCadastrados(lat, lng, raio);
    }

    // Busca o telefone de um salão específico (pin cinza) sob demanda —
    // só o admin autenticado pode chamar isso, nunca o cliente final.
    @GetMapping("/admin/mapa/estabelecimentos-externos/{googlePlaceId}/telefone")
    public Map<String, String> buscarTelefoneExterno(@PathVariable String googlePlaceId) {
        String telefone = googlePlacesService.buscarTelefone(googlePlaceId);
        Map<String, String> resposta = new HashMap<>();
        resposta.put("telefone", telefone);
        return resposta;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Métodos privados
    // ═══════════════════════════════════════════════════════════════════

    private List<SalaoExternoAdminDTO> filtrarNaoCadastrados(double lat, double lng, double raio) {
        List<SalaoExternoAdminDTO> todosDoGoogle = googlePlacesService.buscarSaloesProximos(lat, lng, raio);

        List<Estabelecimento> jaCadastrados = estabelecimentoRepository.buscarProximos(lat, lng, raio);
        List<String> nomesJaCadastrados = jaCadastrados.stream()
                .map(e -> e.getNome().trim().toLowerCase())
                .toList();

        return todosDoGoogle.stream()
                .filter(externo -> !nomesJaCadastrados.contains(externo.getNome().trim().toLowerCase()))
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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