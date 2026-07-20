package com.hady.saas.application.controller;

import com.hady.saas.application.dto.ImpersonarResponseDTO;
import com.hady.saas.business.service.ImpersonarService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Protegido via .requestMatchers("/admin-master/**").hasRole("ADMIN_MASTER")
@RestController
@RequestMapping("/admin-master")
public class ImpersonarController {

    private final ImpersonarService impersonarService;

    public ImpersonarController(ImpersonarService impersonarService) {
        this.impersonarService = impersonarService;
    }

    // POST /admin-master/estabelecimentos/{estabelecimentoId}/usuarios/{usuarioId}/impersonar
    @PostMapping("/estabelecimentos/{estabelecimentoId}/usuarios/{usuarioId}/impersonar")
    public ResponseEntity<ImpersonarResponseDTO> impersonar(@PathVariable Long estabelecimentoId,
                                                            @PathVariable Long usuarioId,
                                                            Authentication auth) {
        String adminEmail = auth.getName();
        ImpersonarResponseDTO resposta = impersonarService.impersonar(estabelecimentoId, usuarioId, adminEmail);
        return ResponseEntity.ok(resposta);
    }
}