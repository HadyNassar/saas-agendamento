package com.hady.saas.application.controller;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.client.EvolutionApiClient;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/painel/whatsapp")
public class WhatsAppController {

    private final EvolutionApiClient evolutionApiClient;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AuthUtil authUtil;

    public WhatsAppController(EvolutionApiClient evolutionApiClient,
                              EstabelecimentoRepository estabelecimentoRepository,
                              AuthUtil authUtil) {
        this.evolutionApiClient = evolutionApiClient;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.authUtil = authUtil;
    }

    private Estabelecimento getEstab(Authentication auth) {
        return estabelecimentoRepository.findById(authUtil.getEstabelecimentoId(auth))
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Map<String, Object>> status(Authentication auth) {
        Estabelecimento est = getEstab(auth);
        return ResponseEntity.ok(Map.of(
                "conectado", est.isWhatsappConectado(),
                "instanceName", est.getEvolutionInstanceName() != null ? est.getEvolutionInstanceName() : ""
        ));
    }

    @PostMapping("/conectar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Map<String, Object>> conectar(Authentication auth) {
        Estabelecimento est = getEstab(auth);
        String instanceName = "agendara-" + est.getSlug();
        try {
            boolean precisaCriar = est.getEvolutionInstanceName() == null
                    || !evolutionApiClient.instanciaExiste(instanceName);

            if (precisaCriar) {
                evolutionApiClient.criarInstancia(instanceName);
                est.setEvolutionInstanceName(instanceName);
                est.setWhatsappConectado(false);
                estabelecimentoRepository.save(est);
            }
            String qrCode = evolutionApiClient.buscarQrCode(instanceName);
            return ResponseEntity.ok(Map.of("qrcode", qrCode));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao gerar QR Code: " + e.getMessage()));
        }
    }

    @GetMapping("/qrcode")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Map<String, Object>> qrcode(Authentication auth) {
        Estabelecimento est = getEstab(auth);
        if (est.getEvolutionInstanceName() == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Nenhuma instância iniciada"));
        }
        // Mesma proteção do /conectar: se a instância sumiu do lado da Evolution,
        // avisamos claramente em vez de deixar o front tentar buscar QR em loop
        // contra uma instância que não existe mais.
        if (!evolutionApiClient.instanciaExiste(est.getEvolutionInstanceName())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "erro", "Instância não encontrada na Evolution API. Use /conectar novamente para recriá-la."));
        }
        try {
            return ResponseEntity.ok(Map.of("qrcode", evolutionApiClient.buscarQrCode(est.getEvolutionInstanceName())));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/verificar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Map<String, Object>> verificar(Authentication auth) {
        Estabelecimento est = getEstab(auth);
        if (est.getEvolutionInstanceName() == null) return ResponseEntity.ok(Map.of("conectado", false));
        boolean conectado = evolutionApiClient.verificarConexao(est.getEvolutionInstanceName());
        if (conectado && !est.isWhatsappConectado()) {
            est.setWhatsappConectado(true);
            estabelecimentoRepository.save(est);
        }
        return ResponseEntity.ok(Map.of("conectado", conectado));
    }

    @PostMapping("/desconectar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Map<String, Object>> desconectar(Authentication auth) {
        Estabelecimento est = getEstab(auth);
        if (est.getEvolutionInstanceName() != null) {
            evolutionApiClient.deletarInstancia(est.getEvolutionInstanceName());
            est.setEvolutionInstanceName(null);
            est.setWhatsappConectado(false);
            estabelecimentoRepository.save(est);
        }
        return ResponseEntity.ok(Map.of("mensagem", "WhatsApp desconectado com sucesso"));
    }
}