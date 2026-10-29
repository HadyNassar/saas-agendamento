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
            } else {
                // Instância já existia: garante que o webhook continua
                // configurado. Já vimos em produção a instância perder essa
                // configuração (ex.: reinício/recriação interna do Evolution)
                // sem que nada no nosso lado soubesse — isso fazia o bot parar
                // de responder silenciosamente, sem erro algum. Reforçar aqui
                // é barato e evita esse silêncio.
                evolutionApiClient.configurarWebhook(instanceName);
            }
            String qrCode = evolutionApiClient.buscarQrCode(instanceName);
            return ResponseEntity.ok(Map.of("qrcode", qrCode));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao gerar QR Code: " + e.getMessage()));
        }
    }

    @PostMapping("/conectar-codigo")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Map<String, Object>> conectarCodigo(Authentication auth,
                                                              @RequestBody Map<String, String> body) {
        String telefone = body.getOrDefault("telefone", "").replaceAll("\\D", "");
        if (telefone.length() < 10) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Número de telefone inválido."));
        }

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
            } else {
                evolutionApiClient.configurarWebhook(instanceName);
            }

            String pairingCode;
            try {
                pairingCode = evolutionApiClient.buscarPairingCode(instanceName, telefone);
            } catch (Exception primeiraTentativa) {
                // Se a instância já tinha gerado um QR antes, a Evolution às vezes
                // não devolve o pairingCode. Recriar a instância "zera" o estado.
                evolutionApiClient.deletarInstancia(instanceName);
                evolutionApiClient.criarInstancia(instanceName);
                est.setEvolutionInstanceName(instanceName);
                est.setWhatsappConectado(false);
                estabelecimentoRepository.save(est);
                pairingCode = evolutionApiClient.buscarPairingCode(instanceName, telefone);
            }

            return ResponseEntity.ok(Map.of("pairingCode", pairingCode));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("erro", "Erro ao gerar código: " + e.getMessage()));
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
//teste