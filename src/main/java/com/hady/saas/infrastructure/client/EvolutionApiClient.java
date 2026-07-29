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

    // URL pública desta própria API/backend (ex.: https://api.oagendara.com.br),
    // usada para montar automaticamente a URL de webhook ao criar uma instância.
    // ATENÇÃO: não usar "app.base-url" — essa property já existe no projeto e
    // aponta para o domínio do FRONTEND (oagendara.com.br), não para esta API.
    @Value("${app.backend-url}")
    private String backendUrl;

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

        // Configura o webhook imediatamente após criar a instância. Isso é
        // crítico: sem isso, toda vez que a instância é recriada (o que já
        // aconteceu mais de uma vez em produção — reinício, reconexão, etc.),
        // a configuração de webhook se perde e o bot conversacional para de
        // responder silenciosamente, sem nenhum erro visível no backend
        // (porque o evento nem chega até ele).
        configurarWebhook(instanceName);
    }

    /**
     * Configura (ou reconfigura) o webhook da instância para apontar para o
     * endpoint do backend, usando o token de autenticação no path da URL
     * (ver EvolutionWebhook). Chamado automaticamente ao criar instância, mas
     * também pode ser chamado manualmente para corrigir uma instância que
     * ficou sem webhook por qualquer motivo.
     */
    public void configurarWebhook(String instanceName) {
        try {
            String url = baseUrl + "/webhook/set/" + instanceName;
            String webhookUrl = backendUrl + "/webhook/evolution/" + apiKey;
            Map<String, Object> body = Map.of(
                    "url", webhookUrl,
                    "enabled", true,
                    "webhook_by_events", false,
                    "events", java.util.List.of("MESSAGES_UPSERT")
            );
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers()), Map.class);
            log.info("[EvolutionAPI] Webhook configurado para instância '{}'.", instanceName);
        } catch (Exception e) {
            log.error("[EvolutionAPI] Falha ao configurar webhook da instância '{}': {}", instanceName, e.getMessage(), e);
        }
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

    /**
     * Verifica se a instância REALMENTE existe no servidor Evolution (diferente de
     * verificarConexao, que só diz se está com sessão "open"). Necessário porque a
     * instância pode ter sido perdida no lado do Evolution (reinício de container,
     * sessão expirada e removida, etc.) mesmo que nosso banco ainda tenha o nome
     * salvo em evolutionInstanceName — nesse caso connectionState pode até responder
     * sem erro (ex.: state "close"), mas gerar QR code vai falhar/ficar em loop
     * porque não há instância de fato para conectar.
     *
     * IMPORTANTE: não usar o filtro via query param (?instanceName=), pois nessa
     * imagem/versão a resposta muda de formato conforme o resultado — quando a
     * instância NÃO existe, retorna um objeto de erro 404 em vez de lista vazia,
     * e quando existe pode retornar um objeto único em vez de lista. Por isso
     * usamos sempre a listagem completa (que consistentemente retorna um array,
     * mesmo vazio) e filtramos pelo nome no lado do backend.
     */
    public boolean instanciaExiste(String instanceName) {
        try {
            String url = baseUrl + "/instance/fetchInstances";
            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), Object.class
            );
            Object body = response.getBody();
            if (!(body instanceof java.util.List<?> lista)) {
                return false;
            }
            for (Object item : lista) {
                String nomeEncontrado = extrairNomeInstancia(item);
                if (instanceName.equals(nomeEncontrado)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("[EvolutionAPI] Falha ao verificar existencia da instancia '{}': {}", instanceName, e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String extrairNomeInstancia(Object item) {
        if (!(item instanceof Map)) return null;
        Map<String, Object> map = (Map<String, Object>) item;

        // Algumas versões retornam o nome direto no objeto, outras aninhado em "instance".
        if (map.get("instanceName") instanceof String nome) return nome;
        if (map.get("name") instanceof String nome) return nome;

        Object instanceObj = map.get("instance");
        if (instanceObj instanceof Map) {
            Map<String, Object> instanceMap = (Map<String, Object>) instanceObj;
            if (instanceMap.get("instanceName") instanceof String nome) return nome;
            if (instanceMap.get("name") instanceof String nome) return nome;
        }
        return null;
    }

    /**
     * Busca a apikey ESPECÍFICA da instância (não a global). Necessária porque a
     * Evolution reenvia, no header "apikey" das chamadas de webhook, a apikey
     * individual da instância — não a apikey global configurada em
     * AUTHENTICATION_API_KEY. Essa apikey de instância é gerada automaticamente
     * pela Evolution a cada criação/recriação de instância, então validar contra
     * um valor fixo (${evolution.api.key}) quebra sempre que a instância é
     * recriada. Buscando dinamicamente aqui, o backend sempre compara contra o
     * valor real e atual, sem precisar de nenhuma configuração manual.
     */
    public String buscarApiKeyInstancia(String instanceName) {
        try {
            String url = baseUrl + "/instance/fetchInstances";
            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), Object.class
            );
            Object body = response.getBody();
            if (!(body instanceof java.util.List<?> lista)) {
                return null;
            }
            for (Object item : lista) {
                if (instanceName.equals(extrairNomeInstancia(item))) {
                    return extrairApiKeyInstancia(item);
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("[EvolutionAPI] Falha ao buscar apikey da instancia '{}': {}", instanceName, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extrairApiKeyInstancia(Object item) {
        if (!(item instanceof Map)) return null;
        Map<String, Object> map = (Map<String, Object>) item;

        if (map.get("apikey") instanceof String key) return key;

        Object instanceObj = map.get("instance");
        if (instanceObj instanceof Map) {
            Map<String, Object> instanceMap = (Map<String, Object>) instanceObj;
            if (instanceMap.get("apikey") instanceof String key) return key;
        }

        Object hashObj = map.get("hash");
        if (hashObj instanceof Map) {
            Map<String, Object> hashMap = (Map<String, Object>) hashObj;
            if (hashMap.get("apikey") instanceof String key) return key;
        }
        return null;
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