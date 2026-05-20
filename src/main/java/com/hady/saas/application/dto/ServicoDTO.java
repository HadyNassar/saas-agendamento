package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ServicoDTO {

    @NotBlank(message = "Nome obrigatório")
    private String nome;

    private String descricao;

    @NotNull(message = "Duração obrigatória")
    @Positive(message = "Duração deve ser maior que zero")
    private Integer duracaoMinutos;

    @NotNull(message = "Preço obrigatório")
    @Positive(message = "Preço deve ser maior que zero")
    private BigDecimal preco;

    private String categoria;

    // GETTERS
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public BigDecimal getPreco() { return preco; }
    public String getCategoria() { return categoria; }

    // SETTERS
    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}
