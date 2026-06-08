package com.hady.saas.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para PUT /painel/assinatura/plano
 *
 * Usado tanto para upgrade quanto para downgrade de plano recorrente.
 * Requer que o estabelecimento já possua uma assinatura recorrente ativa.
 */
public class TrocarPlanoDTO {

    @NotBlank(message = "Plano obrigatório")
    private String plano;       // STANDARD | ELITE | PLATINUM

    @NotBlank(message = "Token do cartão obrigatório")
    private String cardToken;   // token gerado pelo MP SDK no frontend

    @Email(message = "E-mail inválido")
    @NotBlank(message = "E-mail obrigatório")
    private String email;

    public String getPlano()         { return plano; }
    public void   setPlano(String v) { this.plano = v; }

    public String getCardToken()         { return cardToken; }
    public void   setCardToken(String v) { this.cardToken = v; }

    public String getEmail()         { return email; }
    public void   setEmail(String v) { this.email = v; }
}
