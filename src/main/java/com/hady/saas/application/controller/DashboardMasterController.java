package com.hady.saas.application.controller;

import com.hady.saas.application.dto.DashboardMasterDTO;
import com.hady.saas.business.service.DashboardMasterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Dashboard consolidado do dono do SaaS — métricas de TODOS os
// estabelecimentos. Protegido em SecurityConfig via
// .requestMatchers("/admin-master/**").hasRole("ADMIN_MASTER")
@RestController
@RequestMapping("/admin-master/dashboard")
public class DashboardMasterController {

    private final DashboardMasterService dashboardMasterService;

    public DashboardMasterController(DashboardMasterService dashboardMasterService) {
        this.dashboardMasterService = dashboardMasterService;
    }

    @GetMapping
    public DashboardMasterDTO dashboard() {
        return dashboardMasterService.gerarDashboard();
    }
}