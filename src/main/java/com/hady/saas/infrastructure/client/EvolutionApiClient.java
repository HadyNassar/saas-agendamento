package com.hady.saas.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class EvolutionApiClient {

    private static final Logger log = LoggerFactory.getLogger(EvolutionApiClient.class);

    private final RestTemplate restTemplate;

    @Value("${evolution.api.url}")
    private String baseUrl;

    @Value("${evolution.api.key}")
    private String apiKey;

    public EvolutionApiClient() {
        this.restTemplate = new RestTemplate();
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("apikey", apiKey);
        return h;
    }

    public void criarInstancia(String instanceName) {
        String url = baseUrl + "/instance/create";
        Map<String, Object> body = Map.of(
                "instanceName", instanceName,
                "qrcode", true,
                "integration", "WHATSAPP-BAILEYS"
        );
        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers()), Map.class);
    }

    public String buscarQrCode(String instanceName) {
        String url = baseUrl + "/instance/connect/" + instanceName;
        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers()), Map.class
        );
        Map<String, Object> body = response.getBody();
        if (body == null) throw new RuntimeException("Resposta vazia da Evolution API");

        if (body.containsKey("base64")) return (String) body.get("base64");
        if (body.containsKey("code"))   return (String) body.get("code");

        throw new RuntimeException("QR Code não encontrado na resposta");
    }

    public boolean verificarConexao(String instanceName) {
        try {
            String url = baseUrl + "/instance/connectionState/" + instanceName;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), Map.class
            );
            Map<String, Object> body = response.getBody();
            if (body == null) return false;

            Object instance = body.get("instance");
            if (instance instanceof Map<?, ?> inst) {
                return "open".equals(inst.get("state"));
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void deletarInstancia(String instanceName) {
        try {
            String url = baseUrl + "/instance/delete/" + instanceName;
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers()), Map.class);
        } catch (Exception e) {
            log.error("[EvolutionAPI] Falha ao deletar instância '{}': {}", instanceName, e.getMessage(), e);
        }
    }

    public void enviarMensagem(String instanceName, String telefone, String mensagem) {
        String url = baseUrl + "/message/sendText/" + instanceName;

        String numero = telefone.replaceAll("\\D", "");
        if (!numero.startsWith("55")) numero = "55" + numero;

        Map<String, Object> body = Map.of(
                "number", numero,
                "textMessage", Map.of("text", mensagem)
        );

        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers()), Map.class);
    }
}
