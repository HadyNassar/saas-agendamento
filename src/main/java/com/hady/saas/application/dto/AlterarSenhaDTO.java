package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO para trocar senha
public class AlterarSenhaDTO {

    @NotBlank(message = "Senha atual obrigatória")
    private String senhaAtual;

    @NotBlank(message = "Nova senha obrigatória")
    @Size(min = 8, message = "Nova senha deve ter no mínimo 8 caracteres")
    private String novaSenha;

    @NotBlank(message = "Confirmação da nova senha obrigatória")
    private String confirmarNovaSenha;

    public String getSenhaAtual() { return senhaAtual; }
    public String getNovaSenha() { return novaSenha; }
    public String getConfirmarNovaSenha() { return confirmarNovaSenha; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
    public void setConfirmarNovaSenha(String confirmarNovaSenha) { this.confirmarNovaSenha = confirmarNovaSenha; }
}
