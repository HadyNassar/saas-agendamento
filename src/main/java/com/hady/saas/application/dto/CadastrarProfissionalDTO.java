package com.hady.saas.application.dto;

import com.hady.saas.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// DTO para cadastrar profissional + usuário de login de uma vez.
// Roles permitidas dependem do plano:
//   STANDARD / ELITE  → apenas GERENTE
//   PLATINUM          → ADMIN (até 2 por estabelecimento) ou PROFISSIONAL
// A validação de role pelo plano é feita em UsuarioService.salvarFuncionario().
public class CadastrarProfissionalDTO {

    // Dados do profissional
    @NotBlank(message = "Nome obrigatório")
    private String nome;

    private String telefone;

    private List<Long> especialidadeIds;

    private List<Long> servicoIds;

    // Dados do usuário de login
    @Email(message = "Email inválido")
    @NotBlank(message = "Email obrigatório")
    private String email;

    @NotBlank(message = "Senha obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String senha;

    @NotNull(message = "Role obrigatória")
    private Role role;

    // GETTERS
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public List<Long> getEspecialidadeIds() { return especialidadeIds; }
    public List<Long> getServicosIds() { return servicoIds; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public Role getRole() { return role; }

    // SETTERS
    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setEspecialidadeIds(List<Long> ids) { this.especialidadeIds = ids; }
    public void setServicosIds(List<Long> ids) { this.servicoIds = ids; }
    public void setEmail(String email) { this.email = email; }
    public void setSenha(String senha) { this.senha = senha; }
    public void setRole(Role role) { this.role = role; }
}
