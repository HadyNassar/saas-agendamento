package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotBlank;

// DTO para editar dados do estabelecimento
public class EditarEstabelecimentoDTO {

    @NotBlank(message = "Nome obrigatório")
    private String nome;

    private String telefone;
    private String endereco;

    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getEndereco() { return endereco; }
    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
}
