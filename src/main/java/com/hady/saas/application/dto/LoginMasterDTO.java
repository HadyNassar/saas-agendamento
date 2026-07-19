package com.hady.saas.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Passo 1 do login do admin master: email + senha.
// Se corretos, um código de 6 dígitos é enviado por email.
public class LoginMasterDTO {

    @Email(message = "Email inválido")
    @NotBlank(message = "Email obrigatório")
    private String email;

    @NotBlank(message = "Senha obrigatória")
    private String senha;

    public String getEmail()             { return email; }
    public void setEmail(String email)   { this.email = email; }

    public String getSenha()             { return senha; }
    public void setSenha(String senha)   { this.senha = senha; }
}