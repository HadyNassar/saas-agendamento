package com.hady.saas.application.dto;

import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.enums.TipoEstabelecimento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// DTO usado no POST /auth/registro
// Cria o estabelecimento + usuário ADMIN de uma vez
public class RegistroDTO {

    // Dados do usuário ADMIN (dono)
    @NotBlank(message = "Nome obrigatório")
    private String nome;

    @Email(message = "Email inválido")
    @NotBlank(message = "Email obrigatório")
    private String email;

    @NotBlank(message = "Senha obrigatória")
    private String senha;

    // Dados do estabelecimento
    @NotBlank(message = "Nome do estabelecimento obrigatório")
    private String nomeEstabelecimento;

    @NotNull(message = "Tipo do estabelecimento obrigatório")
    private TipoEstabelecimento tipoEstabelecimento;

    @NotNull(message = "Plano obrigatório")
    private Plano plano; // BASE, PREMIUM ou TOP

    private String telefone;
    private String endereco;

    // GETTERS
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public String getNomeEstabelecimento() { return nomeEstabelecimento; }
    public TipoEstabelecimento getTipoEstabelecimento() { return tipoEstabelecimento; }
    public Plano getPlano() { return plano; }
    public String getTelefone() { return telefone; }
    public String getEndereco() { return endereco; }

    // SETTERS
    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }
    public void setSenha(String senha) { this.senha = senha; }
    public void setNomeEstabelecimento(String nomeEstabelecimento) { this.nomeEstabelecimento = nomeEstabelecimento; }
    public void setTipoEstabelecimento(TipoEstabelecimento tipoEstabelecimento) { this.tipoEstabelecimento = tipoEstabelecimento; }
    public void setPlano(Plano plano) { this.plano = plano; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
}
