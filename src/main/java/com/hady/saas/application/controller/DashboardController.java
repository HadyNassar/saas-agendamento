package com.hady.saas.application.controller;

import com.hady.saas.business.service.DashboardService;
import com.hady.saas.business.service.UsuarioService;
import com.hady.saas.domain.usuario.Usuario;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/painel/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UsuarioService usuarioService;

    public DashboardController(DashboardService dashboardService, UsuarioService usuarioService) {
        this.dashboardService = dashboardService;
        this.usuarioService = usuarioService;
    }

    // GET /painel/dashboard?inicio=2026-05-01&fim=2026-05-31
    // Apenas ADMIN acessa — FUNCIONARIO e FUNCIONARIO_TOP não veem dashboard financeiro
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> dashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        Long estabelecimentoId = usuario.getEstabelecimento().getId();

        Map<String, Object> dados = dashboardService.gerarDashboard(estabelecimentoId, inicio, fim);
        return ResponseEntity.ok(dados);
    }

    // GET /painel/dashboard/hoje — atalho para o dia atual
    @GetMapping("/hoje")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> dashboardHoje(
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        LocalDate hoje = LocalDate.now();
        Map<String, Object> dados = dashboardService.gerarDashboard(
                usuario.getEstabelecimento().getId(), hoje, hoje);
        return ResponseEntity.ok(dados);
    }

    // GET /painel/dashboard/mes — mês atual
    @GetMapping("/mes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> dashboardMes(
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());

        Map<String, Object> dados = dashboardService.gerarDashboard(
                usuario.getEstabelecimento().getId(), inicioMes, fimMes);
        return ResponseEntity.ok(dados);
    }
}
