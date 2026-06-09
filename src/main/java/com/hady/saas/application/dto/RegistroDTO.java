package com.hady.saas.application.dto;

import com.hady.saas.domain.enums.TipoEstabelecimento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// DTO usado no POST /auth/registro
// Cria o estabelecimento + usuário ADMIN de uma vez.
// O plano cadastral é sempre STANDARD — upgrade ocorre via pagamento.
// Durante o trial gratuito de 5 dias, o acesso é equivalente ao ELITE
// (limites e roles do plano intermediário), independentemente do plano cadastral.
// Após o trial, o usuário escolhe o plano com base no que já usou.
public class RegistroDTO {

    // Dados do usuário ADMIN (dono)
    @NotBlank(message = "Nome obrigatório")
    private String nome;

    @Email(message = "Email inválido")
    @NotBlank(message = "Email obrigatório")
    private String email;

    @NotBlank(message = "Senha obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String senha;

    // Dados do estabelecimento
    @NotBlank(message = "Nome do estabelecimento obrigatório")
    private String nomeEstabelecimento;

    @NotNull(message = "Tipo do estabelecimento obrigatório")
    private TipoEstabelecimento tipoEstabelecimento;

    // Plano removido — todo cadastro começa em STANDARD.
    // Upgrade de plano ocorre exclusivamente via /painel/assinatura.

    private String telefone;
    private String endereco;

    // GETTERS
    public String getNome()                              { return nome; }
    public String getEmail()                             { return email; }
    public String getSenha()                             { return senha; }
    public String getNomeEstabelecimento()               { return nomeEstabelecimento; }
    public TipoEstabelecimento getTipoEstabelecimento()  { return tipoEstabelecimento; }
    public String getTelefone()                          { return telefone; }
    public String getEndereco()                          { return endereco; }

    // SETTERS
    public void setNome(String nome)                                        { this.nome = nome; }
    public void setEmail(String email)                                      { this.email = email; }
    public void setSenha(String senha)                                      { this.senha = senha; }
    public void setNomeEstabelecimento(String v)                            { this.nomeEstabelecimento = v; }
    public void setTipoEstabelecimento(TipoEstabelecimento v)               { this.tipoEstabelecimento = v; }
    public void setTelefone(String telefone)                                { this.telefone = telefone; }
    public void setEndereco(String endereco)                                { this.endereco = endereco; }
}
