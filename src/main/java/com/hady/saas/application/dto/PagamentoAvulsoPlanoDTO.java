package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotBlank;

// DTO para POST /painel/assinatura/avulso (cartão) e POST /painel/assinatura/avulso-pix
public class PagamentoAvulsoPlanoDTO {

    @NotBlank(message = "plano é obrigatório")
    private String plano;

    // Cartão
    private String cardToken;
    private String email;
    private String paymentMethodId;

    // PIX — e-mail do pagador
    private String pixEmail;

    public String getPlano()               { return plano; }
    public void setPlano(String v)         { this.plano = v; }

    public String getCardToken()           { return cardToken; }
    public void setCardToken(String v)     { this.cardToken = v; }

    public String getEmail()               { return email; }
    public void setEmail(String v)         { this.email = v; }

    public String getPaymentMethodId()     { return paymentMethodId; }
    public void setPaymentMethodId(String v) { this.paymentMethodId = v; }

    public String getPixEmail()            { return pixEmail; }
    public void setPixEmail(String v)      { this.pixEmail = v; }
}