package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO usado pelo admin master para definir uma nova senha para um usuário
// (ex: cliente que esqueceu a senha e não consegue recuperar por e-mail).
public class RedefinirSenhaAdminDTO {

    @NotBlank(message = "Nova senha obrigatória")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    private String novaSenha;

    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
}
