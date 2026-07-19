package com.hady.saas.application.controller;

import com.hady.saas.application.dto.LeadAgrupadoDTO;
import com.hady.saas.application.dto.LeadMapaDTO;
import com.hady.saas.application.dto.MapaMetricaEstabelecimentoDTO;
import com.hady.saas.business.service.GestaoMapaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Gestão do Mapa Público pelo admin master — métricas por estabelecimento,
// controle de visibilidade, e leads capturados de salões não cadastrados.
// Protegido via .requestMatchers("/admin-master/**").hasRole("ADMIN_MASTER")
@RestController
@RequestMapping("/admin-master/mapa")
public class GestaoMapaController {

    private final GestaoMapaService gestaoMapaService;

    public GestaoMapaController(GestaoMapaService gestaoMapaService) {
        this.gestaoMapaService = gestaoMapaService;
    }

    // Métricas de visualização de todos os estabelecimentos cadastrados.
    @GetMapping("/metricas")
    public List<MapaMetricaEstabelecimentoDTO> listarMetricas() {
        return gestaoMapaService.listarMetricas();
    }

    // Ativa ou oculta um estabelecimento do mapa público, sem afetar o
    // resto da conta (agenda, clientes continuam funcionando normalmente).
    @PatchMapping("/estabelecimentos/{id}/visibilidade")
    public ResponseEntity<String> alterarVisibilidade(@PathVariable Long id,
                                                      @RequestBody Map<String, Boolean> body) {
        Boolean visivel = body.get("visivel");
        if (visivel == null) {
            return ResponseEntity.badRequest().body("Campo 'visivel' é obrigatório.");
        }
        gestaoMapaService.alterarVisibilidade(id, visivel);
        return ResponseEntity.ok(visivel ? "Estabelecimento visível no mapa." : "Estabelecimento oculto do mapa.");
    }

    // Lista todos os leads, ou filtra por um salão específico via
    // ?googlePlaceId=... (usado para ver o histórico antes de abordar).
    @GetMapping("/leads")
    public List<LeadMapaDTO> listarLeads(@RequestParam(required = false) String googlePlaceId) {
        return gestaoMapaService.listarLeads(googlePlaceId);
    }

    // Leads agrupados por salão, ordenado por quem tem mais demanda —
    // prioriza quem abordar primeiro na prospecção.
    @GetMapping("/leads/agrupados")
    public List<LeadAgrupadoDTO> listarLeadsAgrupados() {
        return gestaoMapaService.listarLeadsAgrupados();
    }

    // Exporta todos os leads em CSV, pronto para abrir no Excel ou importar
    // em qualquer ferramenta de CRM/planilha de prospecção.
    @GetMapping("/leads/exportar")
    public ResponseEntity<byte[]> exportarLeads() {
        byte[] csv = gestaoMapaService.exportarLeadsCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leads_mapa.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}