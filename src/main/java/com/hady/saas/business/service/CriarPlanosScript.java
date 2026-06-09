package com.hady.saas.business.service;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Script utilitário — roda UMA VEZ para criar os planos no Mercado Pago.
 *
 * Como executar:
 *   Opção 1 (variável de ambiente):
 *     export MP_ACCESS_TOKEN="APP_USR-..."
 *     java -cp ... com.hady.saas.business.service.CriarPlanosScript
 *
 *   Opção 2 (argumento direto):
 *     java -cp ... com.hady.saas.business.service.CriarPlanosScript "APP_USR-..."
 *
 * NUNCA coloque o token diretamente no código-fonte.
 */
public class CriarPlanosScript {

    private static final String MP_API = "https://api.mercadopago.com";

    public static void main(String[] args) {

        // Prioridade: argumento da linha de comando > variável de ambiente
        String accessToken = null;

        if (args.length > 0 && !args[0].isBlank()) {
            accessToken = args[0].trim();
            System.out.println("Usando token fornecido via argumento.\n");
        } else {
            accessToken = System.getenv("MP_ACCESS_TOKEN");
            if (accessToken != null && !accessToken.isBlank()) {
                System.out.println("Usando token da variável de ambiente MP_ACCESS_TOKEN.\n");
            }
        }

        if (accessToken == null || accessToken.isBlank()) {
            System.err.println("❌ Token não encontrado!");
            System.err.println("   Defina a variável de ambiente MP_ACCESS_TOKEN ou passe o token como argumento.");
            System.err.println("   Exemplo: java ... CriarPlanosScript \"APP_USR-SeuTokenAqui\"");
            System.exit(1);
        }

        RestTemplate restTemplate = new RestTemplate();
        System.out.println("Criando planos no Mercado Pago...\n");

        criarPlano(restTemplate, accessToken, "Agendara Standard", new BigDecimal("49.00"));
        criarPlano(restTemplate, accessToken, "Agendara Elite",    new BigDecimal("69.00"));
        criarPlano(restTemplate, accessToken, "Agendara Platinum", new BigDecimal("119.00"));

        System.out.println("\n✅ Copie os IDs acima para o application-local.yml");
    }

    @SuppressWarnings("unchecked")
    private static void criarPlano(RestTemplate restTemplate, String accessToken,
                                   String nome, BigDecimal valor) {
        try {
            Map<String, Object> autoRecurring = new HashMap<>();
            autoRecurring.put("frequency", 1);
            autoRecurring.put("frequency_type", "months");
            autoRecurring.put("transaction_amount", valor);
            autoRecurring.put("currency_id", "BRL");

            Map<String, Object> body = new HashMap<>();
            body.put("reason", nome);
            body.put("auto_recurring", autoRecurring);
            body.put("back_url", System.getenv().getOrDefault(
                    "MP_BACK_URL", "https://seusite.com.br/painel/mercadopago/callback"));
            body.put("status", "active");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    MP_API + "/preapproval_plan", request, Map.class);

            Map<String, Object> data = response.getBody();
            String id = data != null ? (String) data.get("id") : "ERRO";
            System.out.println("📦 " + nome + "  →  ID: " + id);

        } catch (Exception e) {
            System.err.println("❌ Erro ao criar plano '" + nome + "': " + e.getMessage());
        }
    }
}