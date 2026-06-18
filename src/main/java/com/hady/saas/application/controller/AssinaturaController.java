package com.hady.saas.application.controller;

import com.hady.saas.application.dto.AssinaturaDTO;
import com.hady.saas.application.dto.PagamentoAvulsoPlanoDTO;
import com.hady.saas.application.dto.TrocarPlanoDTO;
import com.hady.saas.business.service.AssinaturaService;
import com.hady.saas.infrastructure.security.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/painel/assinatura")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;
    private final AuthUtil authUtil;

    public AssinaturaController(AssinaturaService assinaturaService, AuthUtil authUtil) {
        this.assinaturaService = assinaturaService;
        this.authUtil = authUtil;
    }

    // GET /painel/assinatura/status
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(Authentication auth) {
        Long id = authUtil.getEstabelecimentoId(auth);
        return ResponseEntity.ok(assinaturaService.statusPlano(id));
    }

    /**
     * POST /painel/assinatura/recorrente
     * Primeira assinatura — para estabelecimentos sem plano ativo.
     */
    @PostMapping("/recorrente")
    public ResponseEntity<Map<String, Object>> assinarRecorrente(
            Authentication auth,
            @Valid @RequestBody AssinaturaDTO dto) {
        Long id = authUtil.getEstabelecimentoId(auth);
        return ResponseEntity.ok(assinaturaService.assinarRecorrente(id, dto));
    }

    /**
     * PUT /painel/assinatura/plano
     * Upgrade ou downgrade de plano para assinantes recorrentes já ativos.
     *
     * Diferença semântica em relação ao POST /recorrente:
     *  - POST → cria uma assinatura do zero (sem plano ativo).
     *  - PUT  → troca o plano de uma assinatura já existente,
     *           validando que o novo plano é diferente do atual.
     *
     * Resposta inclui planoAnterior, planoNovo, subscriptionId, validoAte.
     */
    @PutMapping("/plano")
    public ResponseEntity<Map<String, Object>> trocarPlano(
            Authentication auth,
            @Valid @RequestBody TrocarPlanoDTO dto) {
        Long id = authUtil.getEstabelecimentoId(auth);
        return ResponseEntity.ok(assinaturaService.trocarPlano(id, dto));
    }

    // POST /painel/assinatura/avulso
    @PostMapping("/avulso")
    public ResponseEntity<Map<String, Object>> pagarAvulso(
            Authentication auth,
            @Valid @RequestBody PagamentoAvulsoPlanoDTO dto) {
        Long id = authUtil.getEstabelecimentoId(auth);
        return ResponseEntity.ok(assinaturaService.pagarAvulso(id, dto));
    }

    // POST /painel/assinatura/avulso-pix
    @PostMapping("/avulso-pix")
    public ResponseEntity<Map<String, Object>> pagarAvulsoPix(
            Authentication auth,
            @Valid @RequestBody PagamentoAvulsoPlanoDTO dto) {
        Long id = authUtil.getEstabelecimentoId(auth);
        return ResponseEntity.ok(assinaturaService.pagarAvulsoPix(id, dto));
    }

    // DELETE /painel/assinatura
    @DeleteMapping
    public ResponseEntity<Void> cancelar(Authentication auth) {
        Long id = authUtil.getEstabelecimentoId(auth);
        assinaturaService.cancelarAssinatura(id);
        return ResponseEntity.noContent().build();
    }
}