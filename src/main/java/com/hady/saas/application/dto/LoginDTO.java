package com.hady.saas.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// DTO usado no POST /auth/login
public class LoginDTO {

    @Email(message = "Email inválido")
    @NotBlank(message = "Email obrigatório")
    private String email;

    @NotBlank(message = "Senha obrigatória")
    private String senha;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
