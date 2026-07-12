package com.hady.saas.application.controller;

import com.hady.saas.business.service.ConfiguracaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminGeocodingController {

    private final ConfiguracaoService configuracaoService;

    public AdminGeocodingController(ConfiguracaoService configuracaoService) {
        this.configuracaoService = configuracaoService;
    }

    // Endpoint para popular coordenadas de todos os estabelecimentos existentes
    // Chamar uma única vez após o deploy: POST /admin/geocodificar-todos
    @PostMapping("/geocodificar-todos")
    public ResponseEntity<Map<String, Object>> geocodificarTodos() {
        int atualizados = configuracaoService.geocodificarTodos();
        return ResponseEntity.ok(Map.of(
                "mensagem", "Geocodificação concluída",
                "atualizados", atualizados
        ));
    }
}