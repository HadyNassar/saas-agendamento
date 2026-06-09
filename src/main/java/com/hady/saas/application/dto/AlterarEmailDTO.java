package com.hady.saas.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// DTO para trocar email
public class AlterarEmailDTO {

    @Email(message = "Email inválido")
    @NotBlank(message = "Novo email obrigatório")
    private String novoEmail;

    @NotBlank(message = "Senha atual obrigatória para confirmar")
    private String senhaAtual;

    public String getNovoEmail() { return novoEmail; }
    public String getSenhaAtual() { return senhaAtual; }
    public void setNovoEmail(String novoEmail) { this.novoEmail = novoEmail; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
}
