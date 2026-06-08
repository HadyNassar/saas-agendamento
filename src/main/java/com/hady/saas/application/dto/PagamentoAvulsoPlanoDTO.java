package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotBlank;

// DTO para POST /painel/assinatura/avulso
public class PagamentoAvulsoPlanoDTO {

    private String plano;
    private String cardToken;
    private String email;

    // FIX: campo obrigatório — evita fallback hardcoded "master" que rejeitava
    // pagamentos de cartões Visa, Elo, Hipercard etc. O Brick do MP já envia esse valor.
    @NotBlank(message = "paymentMethodId é obrigatório")
    private String paymentMethodId;

    public String getPlano()           { return plano; }
    public void setPlano(String v)     { this.plano = v; }

    public String getCardToken()           { return cardToken; }
    public void setCardToken(String v)     { this.cardToken = v; }

    public String getEmail()           { return email; }
    public void setEmail(String v)     { this.email = v; }

    public String getPaymentMethodId()         { return paymentMethodId; }
    public void setPaymentMethodId(String v)   { this.paymentMethodId = v; }
}