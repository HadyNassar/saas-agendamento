package com.hady.saas.application.controller;

import com.hady.saas.application.dto.EstabelecimentoMapaAdminDTO;
import com.hady.saas.application.dto.LeadAgrupadoDTO;
import com.hady.saas.application.dto.LeadMapaDTO;
import com.hady.saas.application.dto.MapaMetricaEstabelecimentoDTO;
import com.hady.saas.application.dto.SalaoExternoAdminDTO;
import com.hady.saas.business.service.GestaoMapaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Gestão do Mapa Público pelo admin master — métricas por estabelecimento,
// controle de visibilidade, leads capturados, e a própria visão do mapa
// completo (cadastrados + externos) com telefone/WhatsApp liberado.
// Protegido via .requestMatchers("/admin-master/**").hasRole("ADMIN_MASTER")
@RestController
@RequestMapping("/admin-master/mapa")
public class GestaoMapaController {

    private final GestaoMapaService gestaoMapaService;

    public GestaoMapaController(GestaoMapaService gestaoMapaService) {
        this.gestaoMapaService = gestaoMapaService;
    }

    // NOVO — pins verdes pro painel: todos os estabelecimentos cadastrados
    // próximos, incluindo os ocultos do mapa público, já com telefone/WhatsApp.
    @GetMapping("/estabelecimentos")
    public List<EstabelecimentoMapaAdminDTO> listarEstabelecimentos(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double raio) {
        return gestaoMapaService.listarEstabelecimentosParaMapa(lat, lng, raio);
    }

    // MOVIDO do MapaPublicoController — estava em /admin/mapa/** (protegido
    // só por "estar logado", não por role). Agora fica corretamente sob
    // /admin-master/**, exclusivo admin master.
    @GetMapping("/estabelecimentos-externos")
    public List<SalaoExternoAdminDTO> listarExternos(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double raio) {
        return gestaoMapaService.listarExternosParaMapa(lat, lng, raio);
    }

    // MOVIDO do MapaPublicoController — mesmo motivo acima.
    @GetMapping("/estabelecimentos-externos/{googlePlaceId}/telefone")
    public Map<String, String> buscarTelefoneExterno(@PathVariable String googlePlaceId) {
        String telefone = gestaoMapaService.buscarTelefoneExterno(googlePlaceId);
        Map<String, String> resposta = new HashMap<>();
        resposta.put("telefone", telefone);
        return resposta;
    }

    @GetMapping("/metricas")
    public List<MapaMetricaEstabelecimentoDTO> listarMetricas() {
        return gestaoMapaService.listarMetricas();
    }

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

    @GetMapping("/leads")
    public List<LeadMapaDTO> listarLeads(@RequestParam(required = false) String googlePlaceId) {
        return gestaoMapaService.listarLeads(googlePlaceId);
    }

    @GetMapping("/leads/agrupados")
    public List<LeadAgrupadoDTO> listarLeadsAgrupados() {
        return gestaoMapaService.listarLeadsAgrupados();
    }

    @GetMapping("/leads/exportar")
    public ResponseEntity<byte[]> exportarLeads() {
        byte[] csv = gestaoMapaService.exportarLeadsCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leads_mapa.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}