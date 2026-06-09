package com.hady.saas.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

// Versão simplificada — não usa mais MercadoPagoConfig.setAccessToken()
// porque todas as chamadas agora usam RestTemplate com Bearer token direto
@Configuration
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.app-id}")
    private String appId;

    @Value("${mercadopago.client-secret}")
    private String clientSecret;

    @Value("${mercadopago.redirect-uri}")
    private String redirectUri;

    // Secret da aplicação de Assinaturas
    // Variável de ambiente: MERCADOPAGO_WEBHOOK_SECRET
    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    // Secret da aplicação de Marketplace
    // Variável de ambiente: MERCADOPAGO_WEBHOOK_SECRET_MARKETPLACE
    @Value("${mercadopago.webhook-secret-marketplace:}")
    private String webhookSecretMarketplace;

    @Value("${mercadopago.planos.standard:}")
    private String planoStandardId;

    @Value("${mercadopago.planos.elite:}")
    private String planoEliteId;

    @Value("${mercadopago.planos.platinum:}")
    private String planoPlatinumId;

    public String getAccessToken()               { return accessToken; }
    public String getAppId()                     { return appId; }
    public String getClientSecret()              { return clientSecret; }
    public String getRedirectUri()               { return redirectUri; }
    public String getWebhookSecret()             { return webhookSecret; }
    public String getWebhookSecretMarketplace()  { return webhookSecretMarketplace; }
    public String getPlanoStandardId()           { return planoStandardId; }
    public String getPlanoEliteId()              { return planoEliteId; }
    public String getPlanoPlatinumId()           { return planoPlatinumId; }

    public String getPlanoIdPorTipo(String plano) {
        String planId = switch (plano.toUpperCase()) {
            case "STANDARD" -> planoStandardId;
            case "ELITE"    -> planoEliteId;
            case "PLATINUM" -> planoPlatinumId;
            default -> throw new IllegalArgumentException("Plano inválido: " + plano);
        };
        if (planId == null || planId.isBlank()) {
            throw new IllegalStateException(
                    "ID do plano " + plano.toUpperCase() + " não configurado. " +
                            "Defina a variável mercadopago.planos." + plano.toLowerCase() + " no application.yml."
            );
        }
        return planId;
    }
}