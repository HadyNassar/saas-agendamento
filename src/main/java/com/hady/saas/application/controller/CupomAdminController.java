package com.hady.saas.application.controller;

import com.hady.saas.application.dto.CriarCupomDTO;
import com.hady.saas.application.dto.CupomDTO;
import com.hady.saas.business.service.CupomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Gestão de cupons pelo admin master. Protegido via
// .requestMatchers("/admin-master/**").hasRole("ADMIN_MASTER")
@RestController
@RequestMapping("/admin-master/cupons")
public class CupomAdminController {

    private final CupomService cupomService;

    public CupomAdminController(CupomService cupomService) {
        this.cupomService = cupomService;
    }

    @PostMapping
    public ResponseEntity<CupomDTO> criar(@Valid @RequestBody CriarCupomDTO dto) {
        return ResponseEntity.ok(cupomService.criar(dto));
    }

    @GetMapping
    public List<CupomDTO> listar() {
        return cupomService.listar();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> alterarStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean ativo = body.get("ativo");
        if (ativo == null) {
            return ResponseEntity.badRequest().body("Campo 'ativo' é obrigatório.");
        }
        cupomService.alterarStatus(id, ativo);
        return ResponseEntity.ok(ativo ? "Cupom ativado." : "Cupom desativado.");
    }
}