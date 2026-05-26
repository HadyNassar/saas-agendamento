package com.hady.saas.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class EditarEstabelecimentoDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    private String telefone;
    private String endereco;

    @Min(value = 1, message = "Mínimo de 1 mês")
    @Max(value = 12, message = "Máximo de 12 meses")
    private Integer mesesAntecipacao;

    public EditarEstabelecimentoDTO() {}

    public String getNome()                  { return nome; }
    public void setNome(String nome)         { this.nome = nome; }

    public String getTelefone()              { return telefone; }
    public void setTelefone(String tel)      { this.telefone = tel; }

    public String getEndereco()              { return endereco; }
    public void setEndereco(String end)      { this.endereco = end; }

    public Integer getMesesAntecipacao()     { return mesesAntecipacao; }
    public void setMesesAntecipacao(Integer m) { this.mesesAntecipacao = m; }
}
