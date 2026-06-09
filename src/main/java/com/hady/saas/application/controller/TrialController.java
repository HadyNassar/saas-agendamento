package com.hady.saas.application.controller;

import com.hady.saas.business.service.TrialService;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/painel/trial")
public class TrialController {

    private final TrialService trialService;
    private final AuthUtil authUtil;

    public TrialController(TrialService trialService, AuthUtil authUtil) {
        this.trialService = trialService;
        this.authUtil = authUtil;
    }

    // POST /painel/trial/iniciar — ativa o trial de 5 dias
    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, Object>> iniciar(Authentication auth) {
        Long id = authUtil.getEstabelecimentoId(auth);
        return ResponseEntity.ok(trialService.iniciarTrial(id));
    }

    // GET /painel/trial/status — retorna status do trial
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(Authentication auth) {
        Long id = authUtil.getEstabelecimentoId(auth);
        return ResponseEntity.ok(trialService.statusTrial(id));
    }
}
