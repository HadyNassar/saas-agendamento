package com.hady.saas.application.controller;

import com.hady.saas.application.dto.CobrancaAdminDTO;
import com.hady.saas.application.dto.InadimplenteDTO;
import com.hady.saas.application.dto.ResumoFinanceiroDTO;
import com.hady.saas.business.service.FinanceiroMasterService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// Painel financeiro completo do admin master — receita, MRR, inadimplência,
// histórico de pagamentos. Protegido via hasRole("ADMIN_MASTER") no SecurityConfig.
@RestController
@RequestMapping("/admin-master/financeiro")
public class FinanceiroMasterController {

    private final FinanceiroMasterService financeiroMasterService;

    public FinanceiroMasterController(FinanceiroMasterService financeiroMasterService) {
        this.financeiroMasterService = financeiroMasterService;
    }

    // Resumo com cartões principais — se não passar datas, usa o mês atual.
    // GET /admin-master/financeiro/resumo?inicio=2026-07-01&fim=2026-07-31
    @GetMapping("/resumo")
    public ResumoFinanceiroDTO resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return financeiroMasterService.gerarResumo(inicio, fim);
    }

    // Lista de todas as cobranças, com filtros opcionais.
    @GetMapping("/cobrancas")
    public List<CobrancaAdminDTO> listarCobrancas(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return financeiroMasterService.listarCobrancas(status, inicio, fim);
    }

    // Histórico de pagamentos de um estabelecimento específico.
    @GetMapping("/cobrancas/estabelecimento/{id}")
    public List<CobrancaAdminDTO> listarPorEstabelecimento(@PathVariable Long id) {
        return financeiroMasterService.listarCobrancasPorEstabelecimento(id);
    }

    // Lista de inadimplentes, ordenados por dias em atraso (mais crítico primeiro).
    @GetMapping("/inadimplentes")
    public List<InadimplenteDTO> listarInadimplentes() {
        return financeiroMasterService.listarInadimplentes();
    }

    // Exporta cobranças em CSV.
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        byte[] csv = financeiroMasterService.exportarCobrancasCsv(inicio, fim);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financeiro.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}