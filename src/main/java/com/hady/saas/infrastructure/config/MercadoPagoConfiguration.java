package com.hady.saas.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfiguration {

    // Token da aplicação de MARKETPLACE (split de pagamentos)
    // Variável: MERCADOPAGO_ACCESS_TOKEN
    @Value("${mercadopago.access-token}")
    private String accessToken;

    // Token da aplicação de ASSINATURAS (planos mensais)
    // Variável: MERCADOPAGO_ACCESS_TOKEN_ASSINATURAS
    @Value("${mercadopago.access-token-assinaturas:}")
    private String accessTokenAssinaturas;

    @Value("${mercadopago.app-id}")
    private String appId;

    @Value("${mercadopago.client-secret}")
    private String clientSecret;

    @Value("${mercadopago.redirect-uri}")
    private String redirectUri;

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    @Value("${mercadopago.webhook-secret-marketplace:}")
    private String webhookSecretMarketplace;

    @Value("${mercadopago.planos.standard:}")
    private String planoStandardId;

    @Value("${mercadopago.planos.elite:}")
    private String planoEliteId;

    @Value("${mercadopago.planos.platinum:}")
    private String planoPlatinumId;

    /** Token do Marketplace — usado em split de pagamentos dos clientes */
    public String getAccessToken()               { return accessToken; }

    /** Token de Assinaturas — usado para criar/cancelar planos mensais */
    public String getAccessTokenAssinaturas()    { return accessTokenAssinaturas; }

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