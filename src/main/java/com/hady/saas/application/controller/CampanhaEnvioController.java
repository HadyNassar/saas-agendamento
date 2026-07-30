package com.hady.saas.application.controller;

import com.hady.saas.application.dto.CampanhaEnvioResumoDTO;
import com.hady.saas.application.dto.CampanhaTemplateDTO;
import com.hady.saas.application.dto.ConfigurarCampanhaEnvioDTO;
import com.hady.saas.application.dto.CriarCampanhaEnvioDTO;
import com.hady.saas.application.dto.CupomCampanhaDTO;
import com.hady.saas.business.service.CampanhaEnvioService;
import com.hady.saas.business.service.CampanhaEnvioService.CampanhaEnvioException;
import com.hady.saas.business.service.CampanhaEnvioService.CupomConfig;
import com.hady.saas.business.service.CampanhaTemplateCatalogo;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.whatsapp.CampanhaEnvio;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Disparo de campanha de marketing manual pelo dono do estabelecimento
 * (seção 5-B do documento). Reaproveita a fila técnica de CampanhaEnvio /
 * CampanhaEnvioItem / CampanhaEnvioProcessorJob que já existia pronta —
 * este controller é quem finalmente cria e confirma o disparo.
 *
 * Gating: mesmo padrão do resto do robô, `!= Plano.STANDARD` — ELITE e
 * PLATINUM têm exatamente o mesmo comportamento aqui, sem exceção.
 *
 * Fluxo em duas etapas, por exigência explícita do dono do produto de que o
 * disparo nunca aconteça sem confirmação:
 *   1) POST /painel/whatsapp/campanhas            -> cria um RASCUNHO e
 *      devolve quantos clientes seriam alcançados, sem enviar nada ainda.
 *   2) POST /painel/whatsapp/campanhas/{id}/confirmar -> só agora os itens
 *      entram na fila que o CampanhaEnvioProcessorJob processa.
 * Um rascunho que o dono não quiser mais pode ser descartado via
 * POST /painel/whatsapp/campanhas/{id}/cancelar.
 */
@RestController
@RequestMapping("/painel/whatsapp/campanhas")
public class CampanhaEnvioController {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final CampanhaEnvioService campanhaEnvioService;
    private final CampanhaTemplateCatalogo templateCatalogo;
    private final AuthUtil authUtil;

    public CampanhaEnvioController(EstabelecimentoRepository estabelecimentoRepository,
                                   CampanhaEnvioService campanhaEnvioService,
                                   CampanhaTemplateCatalogo templateCatalogo,
                                   AuthUtil authUtil) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.campanhaEnvioService = campanhaEnvioService;
        this.templateCatalogo = templateCatalogo;
        this.authUtil = authUtil;
    }

    private Estabelecimento getEstab(Authentication auth) {
        return estabelecimentoRepository.findById(authUtil.getEstabelecimentoId(auth))
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }

    // Mesmo padrão de gating do WhatsAppRoboController: bloqueia quem não tem
    // ELITE/PLATINUM (nem trial, que cai em ELITE). Nunca checar == PLATINUM
    // de forma exclusiva.
    private ResponseEntity<Map<String, Object>> bloqueioDePlanoOuNull(Estabelecimento est) {
        if (est.getPlanoEfetivo() == Plano.STANDARD) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("erro", "Campanhas de marketing via WhatsApp estão disponíveis a partir do plano Elite."));
        }
        return null;
    }

    // ── Criar rascunho ───────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> criar(Authentication auth, @RequestBody CriarCampanhaEnvioDTO dto) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        if (dto == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Corpo da requisição é obrigatório."));
        }

        try {
            CupomConfig cupomConfig = paraCupomConfig(dto.getCupom());
            CampanhaEnvio campanha = campanhaEnvioService.criarRascunho(
                    est, dto.getFiltroEstagio(), dto.getMensagem(), dto.getProfissionalIds(), cupomConfig);
            return ResponseEntity.status(HttpStatus.CREATED).body(new CampanhaEnvioResumoDTO(campanha));
        } catch (CampanhaEnvioException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.getMessage()));
        }
    }

    private CupomConfig paraCupomConfig(CupomCampanhaDTO dto) {
        if (dto == null) return null;
        return new CupomConfig(dto.getCodigo(), dto.getTipoDesconto(), dto.getValorDesconto(),
                dto.getDiasValidade(), dto.getLimiteUsos());
    }

    // ── Catálogo de campanhas predefinidas ──────────────────────────────────
    // Front busca aqui pra montar a tela de "escolher um modelo pronto",
    // com os campos de texto livre que cada modelo precisa. Genérico pra
    // qualquer tipo de estabelecimento — ver CampanhaTemplateCatalogo.

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> listarTemplates(Authentication auth) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        List<CampanhaTemplateDTO> templates = templateCatalogo.listar();
        return ResponseEntity.ok(templates);
    }

    // ── Listar / detalhar ────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> listar(Authentication auth) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        List<CampanhaEnvioResumoDTO> resposta = campanhaEnvioService.listar(est).stream()
                .map(CampanhaEnvioResumoDTO::new)
                .toList();
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> detalhar(Authentication auth, @PathVariable Long id) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        try {
            CampanhaEnvio campanha = campanhaEnvioService.buscarDoEstabelecimento(est, id);
            return ResponseEntity.ok(new CampanhaEnvioResumoDTO(campanha));
        } catch (CampanhaEnvioException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    // ── Confirmar disparo ────────────────────────────────────────────────────

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> confirmar(Authentication auth, @PathVariable Long id) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        try {
            CampanhaEnvio campanha = campanhaEnvioService.confirmar(est, id);
            return ResponseEntity.ok(new CampanhaEnvioResumoDTO(campanha));
        } catch (CampanhaEnvioException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.getMessage()));
        }
    }

    // ── Configuração (template universal + intervalo anti-banimento) ───────

    @GetMapping("/configuracao")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> obterConfiguracao(Authentication auth) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        ConfigurarCampanhaEnvioDTO dto = new ConfigurarCampanhaEnvioDTO();
        dto.setTemplateMensagemCampanha(est.getTemplateMensagemCampanha());
        dto.setIntervaloMinimoCampanhaDias(est.getIntervaloMinimoCampanhaDias());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/configuracao")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> salvarConfiguracao(Authentication auth, @RequestBody ConfigurarCampanhaEnvioDTO dto) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        if (dto.getTemplateMensagemCampanha() != null && dto.getTemplateMensagemCampanha().isBlank()) {
            est.setTemplateMensagemCampanha(null);
        } else {
            est.setTemplateMensagemCampanha(dto.getTemplateMensagemCampanha());
        }
        if (dto.getIntervaloMinimoCampanhaDias() != null) {
            est.setIntervaloMinimoCampanhaDias(dto.getIntervaloMinimoCampanhaDias());
        }
        estabelecimentoRepository.save(est);

        ConfigurarCampanhaEnvioDTO resposta = new ConfigurarCampanhaEnvioDTO();
        resposta.setTemplateMensagemCampanha(est.getTemplateMensagemCampanha());
        resposta.setIntervaloMinimoCampanhaDias(est.getIntervaloMinimoCampanhaDias());
        return ResponseEntity.ok(resposta);
    }

    // ── Cancelar rascunho ────────────────────────────────────────────────────

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> cancelar(Authentication auth, @PathVariable Long id) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        try {
            CampanhaEnvio campanha = campanhaEnvioService.cancelar(est, id);
            return ResponseEntity.ok(new CampanhaEnvioResumoDTO(campanha));
        } catch (CampanhaEnvioException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.getMessage()));
        }
    }
}