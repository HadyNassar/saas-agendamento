package com.hady.saas.application.controller;

import com.hady.saas.application.dto.ValidarCupomResponseDTO;
import com.hady.saas.business.service.CupomService;
import org.springframework.web.bind.annotation.*;

// Endpoints usados no fluxo normal de assinatura pelo dono do salão —
// exigem login (JWT de estabelecimento), mas não são exclusivos do
// admin master, por isso ficam fora de /admin-master/**.
@RestController
@RequestMapping("/painel/cupons")
public class CupomPublicoController {

    private final CupomService cupomService;

    public CupomPublicoController(CupomService cupomService) {
        this.cupomService = cupomService;
    }

    // Valida um cupom antes do pagamento (não consome o uso ainda).
    // GET /painel/cupons/validar?codigo=PROMO10&plano=ELITE
    @GetMapping("/validar")
    public ValidarCupomResponseDTO validar(@RequestParam String codigo, @RequestParam String plano) {
        return cupomService.validar(codigo, plano);
    }
}