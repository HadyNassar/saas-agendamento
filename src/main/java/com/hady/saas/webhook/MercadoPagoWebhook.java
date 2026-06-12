package com.hady.saas.webhook;

import com.hady.saas.infrastructure.config.MercadoPagoConfiguration;
import com.hady.saas.infrastructure.repository.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

// ⚠️ Rota pública — liberada no SecurityConfig
@RestController
@RequestMapping("/webhook/mercadopago")
public class MercadoPagoWebhook {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhook.class);
    private static final String MP_API = "https://api.mercadopago.com";

    private final MercadoPagoConfiguration mpConfig;
    private final RestTemplate restTemplate;
    private final WebhookProcessorService webhookProcessor;
    private final TransacaoRepository transacaoRepo;

    public MercadoPagoWebhook(MercadoPagoConfiguration mpConfig,
                              RestTemplate restTemplate,
                              WebhookProcessorService webhookProcessor,
                              TransacaoRepository transacaoRepo) {
        this.mpConfig = mpConfig;
        this.restTemplate = restTemplate;
        this.webhookProcessor = webhookProcessor;
        this.transacaoRepo = transacaoRepo;
    }

    @PostMapping
    public ResponseEntity<Void> receber(
            @RequestHeader(value = "x-signature",   required = false) String xSignature,
            @RequestHeader(value = "x-request-id",  required = false) String xRequestId,
            @RequestBody Map<String, Object> body) {

        String type = (String) body.get("type");

        // Seleciona o secret correto pelo tipo do evento:
        //   subscription_preapproval → aplicação de Assinaturas
        //   payment                  → aplicação de Marketplace
        String secret = resolverSecret(type);

        if (!validarAssinatura(xSignature, xRequestId, body, secret)) {
            log.warn("[Webhook MP] Assinatura inválida — type={} x-signature={}", type, xSignature);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) body.get("data");
            if (dataMap == null || type == null) return ResponseEntity.ok().build();

            String dataId = String.valueOf(dataMap.get("id"));

            switch (type) {
                case "subscription_preapproval" -> {
                    Map<String, Object> sub = buscarRecursoMP("/preapproval/" + dataId, mpConfig.getAccessToken());
                    if (sub != null) {
                        webhookProcessor.atualizarStatusPlano(dataId, (String) sub.get("status"));
                    }
                }
                case "payment" -> {
                    // Busca a transação para descobrir qual estabelecimento (e qual
                    // token OAuth) é dono deste pagamento — o pagamento foi criado
                    // na conta do estabelecimento via split, não na conta principal.
                    String tokenConsulta = mpConfig.getAccessToken();
                    var transacaoOpt = transacaoRepo.findByMpPaymentId(dataId);
                    if (transacaoOpt.isPresent()) {
                        String tokenEstab = transacaoOpt.get().getEstabelecimento().getMpAccessToken();
                        if (tokenEstab != null && !tokenEstab.isBlank()) {
                            tokenConsulta = tokenEstab;
                        }
                    } else {
                        log.warn("[Webhook MP] Transação não encontrada para mpPaymentId={} — " +
                                "consultando com token padrão (pode falhar 403/404)", dataId);
                    }

                    Map<String, Object> payment = buscarRecursoMP("/v1/payments/" + dataId, tokenConsulta);
                    if (payment != null) {
                        webhookProcessor.atualizarStatusTransacao(dataId, (String) payment.get("status"));
                    }
                }
                default -> log.debug("[Webhook MP] Tipo de notificação ignorado: {}", type);
            }
        } catch (Exception e) {
            // Sempre retorna 200 para o MP não retentar em loop
            log.error("[Webhook MP] Erro ao processar notificação", e);
        }

        return ResponseEntity.ok().build();
    }

    // ── Seleciona o secret correto pelo tipo do evento ───────────────────────
    private String resolverSecret(String type) {
        if ("payment".equals(type)) {
            return mpConfig.getWebhookSecretMarketplace();
        }
        // subscription_preapproval e qualquer outro → secret de assinaturas
        return mpConfig.getWebhookSecret();
    }

    // ── Valida assinatura HMAC-SHA256 do Mercado Pago ────────────────────────
    private boolean validarAssinatura(String xSignature, String xRequestId,
                                      Map<String, Object> body, String secret) {

        if (secret == null || secret.isBlank()) {
            log.error("[Webhook MP] Secret não configurado para este tipo de evento — endpoint BLOQUEADO.");
            return false;
        }

        if (xSignature == null || xSignature.isBlank()) {
            return false;
        }

        try {
            String ts = null;
            String v1 = null;
            for (String parte : xSignature.split(",")) {
                String[] kv = parte.trim().split("=", 2);
                if (kv.length == 2) {
                    if ("ts".equals(kv[0]))  ts = kv[1];
                    if ("v1".equals(kv[0]))  v1 = kv[1];
                }
            }

            if (ts == null || v1 == null) return false;

            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) body.get("data");
            String dataId = (dataMap != null) ? String.valueOf(dataMap.get("id")) : "";

            String manifest = "id:" + dataId + ";"
                    + "request-id:" + (xRequestId != null ? xRequestId : "") + ";"
                    + "ts:" + ts + ";";

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            String hashHex = HexFormat.of().formatHex(hash);

            return hashHex.equals(v1);

        } catch (Exception e) {
            log.error("[Webhook MP] Erro ao validar assinatura", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buscarRecursoMP(String path, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    MP_API + path, HttpMethod.GET, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("[Webhook MP] Erro ao buscar {}: {}", path, e.getMessage());
            return null;
        }
    }
}