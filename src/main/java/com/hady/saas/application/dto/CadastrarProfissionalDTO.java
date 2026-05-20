package com.hady.saas.application.dto;

import com.hady.saas.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// DTO para cadastrar profissional + usuário de login de uma vez
public class CadastrarProfissionalDTO {

    // Dados do profissional
    @NotBlank(message = "Nome obrigatório")
    private String nome;

    private String telefone;

    private List<Long> especialidadeIds; // IDs das especialidades já cadastradas

    // Dados do usuário de login
    @Email(message = "Email inválido")
    @NotBlank(message = "Email obrigatório")
    private String email;

    @NotBlank(message = "Senha obrigatória")
    private String senha;

    @NotNull(message = "Role obrigatória")
    private Role role; // FUNCIONARIO ou FUNCIONARIO_TOP

    // GETTERS
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public List<Long> getEspecialidadeIds() { return especialidadeIds; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public Role getRole() { return role; }

    // SETTERS
    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setEspecialidadeIds(List<Long> especialidadeIds) { this.especialidadeIds = especialidadeIds; }
    public void setEmail(String email) { this.email = email; }
    public void setSenha(String senha) { this.senha = senha; }
    public void setRole(Role role) { this.role = role; }
}
