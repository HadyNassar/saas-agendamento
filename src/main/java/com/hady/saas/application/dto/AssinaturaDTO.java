package com.hady.saas.application.dto;

// DTO para POST /painel/assinatura/recorrente
public class AssinaturaDTO {

    private String plano;      // STANDARD | ELITE | PLATINUM
    private String cardToken;  // token gerado pelo MP SDK no frontend
    private String email;      // e-mail do pagador

    public String getPlano()       { return plano; }
    public void setPlano(String v) { this.plano = v; }

    public String getCardToken()       { return cardToken; }
    public void setCardToken(String v) { this.cardToken = v; }

    public String getEmail()       { return email; }
    public void setEmail(String v) { this.email = v; }
}
