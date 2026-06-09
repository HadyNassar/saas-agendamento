package com.hady.saas.application.controller;

import com.hady.saas.business.service.DashboardService;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/painel/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthUtil authUtil;

    public DashboardController(DashboardService dashboardService, AuthUtil authUtil) {
        this.dashboardService = dashboardService;
        this.authUtil = authUtil;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> dashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication auth) {
        return ResponseEntity.ok(dashboardService.gerarDashboard(authUtil.getEstabelecimentoId(auth), inicio, fim));
    }

    @GetMapping("/hoje")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> dashboardHoje(Authentication auth) {
        LocalDate hoje = LocalDate.now();
        return ResponseEntity.ok(dashboardService.gerarDashboard(authUtil.getEstabelecimentoId(auth), hoje, hoje));
    }

    @GetMapping("/mes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> dashboardMes(Authentication auth) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());
        return ResponseEntity.ok(dashboardService.gerarDashboard(authUtil.getEstabelecimentoId(auth), inicioMes, fimMes));
    }
}
