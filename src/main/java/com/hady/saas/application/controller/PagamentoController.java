package com.hady.saas.application.controller;

import com.hady.saas.application.dto.PagamentoAgendamentoDTO;
import com.hady.saas.business.service.MarketplaceOAuthService;
import com.hady.saas.business.service.PagamentoSplitService;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
public class PagamentoController {

    private final PagamentoSplitService splitService;
    private final MarketplaceOAuthService oauthService;
    private final AuthUtil authUtil;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public PagamentoController(PagamentoSplitService splitService,
                               MarketplaceOAuthService oauthService,
                               AuthUtil authUtil) {
        this.splitService = splitService;
        this.oauthService = oauthService;
        this.authUtil = authUtil;
    }

    // ─── OAuth Marketplace ───────────────────────────────────────────────────

    // GET /painel/mercadopago/conectar — retorna a URL de autorização do MP
    @GetMapping("/painel/mercadopago/conectar")
    public ResponseEntity<Map<String, String>> conectar(Authentication auth) {
        Long id = authUtil.getEstabelecimentoId(auth);
        String url = oauthService.gerarUrlAutorizacao(id);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // GET /painel/mercadopago/callback?code=XXX&state=UUID_OPACO
    // ⚠️ Rota pública — liberada no SecurityConfig.
    // CORRIGIDO: state agora é um UUID opaco gerado em gerarUrlAutorizacao(),
    // resolvido para estabelecimentoId via cache com TTL de 10 min.
    // Isso impede que um atacante forje o state com um estabelecimentoId arbitrário.
    @GetMapping("/painel/mercadopago/callback")
    public ResponseEntity<Void> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {   // ← era Long, agora String (UUID opaco)

        Long estabelecimentoId = oauthService.resolverState(state);
        oauthService.processarCallback(code, estabelecimentoId);

        return ResponseEntity.status(302)
                .location(URI.create(frontendUrl + "/painel/planos?mp=conectado"))
                .build();
    }

    // POST /painel/mercadopago/desconectar
    @PostMapping("/painel/mercadopago/desconectar")
    public ResponseEntity<Void> desconectar(Authentication auth) {
        Long id = authUtil.getEstabelecimentoId(auth);
        oauthService.desconectar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/painel/mercadopago/aceitar-pagamento")
    public ResponseEntity<Void> toggleAceitaPagamento(
            Authentication auth,
            @RequestBody Map<String, Boolean> body) {

        Long id = authUtil.getEstabelecimentoId(auth);

        Boolean ativo = body.get("ativo");
        if (ativo == null) throw new RuntimeException("Campo 'ativo' obrigatório");

        splitService.toggleAceitaPagamento(id, ativo);
        return ResponseEntity.ok().build();
    }

    // ─── Pagamento do cliente (público) ──────────────────────────────────────

    // POST /app/{slug}/pagamento
    @PostMapping("/app/{slug}/pagamento")
    public ResponseEntity<Map<String, Object>> pagar(
            @PathVariable String slug,
            @RequestBody PagamentoAgendamentoDTO dto) throws Exception {
        return ResponseEntity.ok(splitService.processarPagamento(slug, dto));
    }

    // GET /app/{slug}/pagamento/{transacaoId}/status — polling do pix
    // CORRIGIDO: slug agora é passado para o service para validar isolamento de tenant.
    @GetMapping("/app/{slug}/pagamento/{transacaoId}/status")
    public ResponseEntity<Map<String, Object>> statusPagamento(
            @PathVariable String slug,
            @PathVariable Long transacaoId) throws Exception {
        return ResponseEntity.ok(splitService.consultarTransacao(transacaoId, slug));
    }
}