package com.hady.saas.application.controller;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.client.EvolutionApiClient;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.business.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints do painel para gerenciar a conexão WhatsApp do estabelecimento.
 *
 * GET  /painel/whatsapp/status    → retorna se está conectado + instanceName
 * POST /painel/whatsapp/conectar  → cria instância e retorna QR Code
 * GET  /painel/whatsapp/qrcode    → busca QR Code atualizado (polling)
 * POST /painel/whatsapp/verificar → verifica se escaneou e atualiza o banco
 * POST /painel/whatsapp/desconectar → desconecta e limpa a instância
 */
@RestController
@RequestMapping("/painel/whatsapp")
public class WhatsAppController {

    private final EvolutionApiClient evolutionApiClient;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final UsuarioService usuarioService;

    public WhatsAppController(EvolutionApiClient evolutionApiClient,
                              EstabelecimentoRepository estabelecimentoRepository,
                              UsuarioService usuarioService) {
        this.evolutionApiClient = evolutionApiClient;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.usuarioService = usuarioService;
    }

    private Estabelecimento getEstabelecimento(UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        return usuario.getEstabelecimento();
    }

    // ─── Status atual ─────────────────────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(
            @AuthenticationPrincipal UserDetails userDetails) {

        Estabelecimento est = getEstabelecimento(userDetails);
        return ResponseEntity.ok(Map.of(
                "conectado", est.isWhatsappConectado(),
                "instanceName", est.getEvolutionInstanceName() != null
                        ? est.getEvolutionInstanceName() : ""
        ));
    }

    // ─── Iniciar conexão — cria instância e retorna QR Code ──────────────────

    @PostMapping("/conectar")
    public ResponseEntity<Map<String, Object>> conectar(
            @AuthenticationPrincipal UserDetails userDetails) {

        Estabelecimento est = getEstabelecimento(userDetails);

        // Nome único da instância: "agendara-{slug}"
        String instanceName = "agendara-" + est.getSlug();

        try {
            // Se já tem instância, tenta só buscar o QR Code
            // Se não tem, cria primeiro
            if (est.getEvolutionInstanceName() == null) {
                evolutionApiClient.criarInstancia(instanceName);
                est.setEvolutionInstanceName(instanceName);
                est.setWhatsappConectado(false);
                estabelecimentoRepository.save(est);
            }

            String qrCode = evolutionApiClient.buscarQrCode(instanceName);
            return ResponseEntity.ok(Map.of("qrcode", qrCode));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("erro", "Erro ao gerar QR Code: " + e.getMessage()));
        }
    }

    // ─── Busca QR Code atualizado (frontend faz polling a cada 3s) ───────────

    @GetMapping("/qrcode")
    public ResponseEntity<Map<String, Object>> qrcode(
            @AuthenticationPrincipal UserDetails userDetails) {

        Estabelecimento est = getEstabelecimento(userDetails);

        if (est.getEvolutionInstanceName() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erro", "Nenhuma instância iniciada"));
        }

        try {
            String qrCode = evolutionApiClient.buscarQrCode(est.getEvolutionInstanceName());
            return ResponseEntity.ok(Map.of("qrcode", qrCode));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    // ─── Verifica se o QR Code foi escaneado e atualiza o banco ─────────────

    @PostMapping("/verificar")
    public ResponseEntity<Map<String, Object>> verificar(
            @AuthenticationPrincipal UserDetails userDetails) {

        Estabelecimento est = getEstabelecimento(userDetails);

        if (est.getEvolutionInstanceName() == null) {
            return ResponseEntity.ok(Map.of("conectado", false));
        }

        boolean conectado = evolutionApiClient.verificarConexao(est.getEvolutionInstanceName());

        if (conectado && !est.isWhatsappConectado()) {
            est.setWhatsappConectado(true);
            estabelecimentoRepository.save(est);
        }

        return ResponseEntity.ok(Map.of("conectado", conectado));
    }

    // ─── Desconectar e limpar instância ──────────────────────────────────────

    @PostMapping("/desconectar")
    public ResponseEntity<Map<String, Object>> desconectar(
            @AuthenticationPrincipal UserDetails userDetails) {

        Estabelecimento est = getEstabelecimento(userDetails);

        if (est.getEvolutionInstanceName() != null) {
            evolutionApiClient.deletarInstancia(est.getEvolutionInstanceName());
            est.setEvolutionInstanceName(null);
            est.setWhatsappConectado(false);
            estabelecimentoRepository.save(est);
        }

        return ResponseEntity.ok(Map.of("mensagem", "WhatsApp desconectado com sucesso"));
    }
}
