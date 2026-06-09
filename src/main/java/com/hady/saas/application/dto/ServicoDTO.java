package com.hady.saas.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ServicoDTO {

    @NotBlank(message = "Nome obrigatório")
    private String nome;

    private String descricao;

    @NotNull(message = "Duração obrigatória")
    // FIX: @Positive não impede valores como 1 ou 2, que gerariam centenas de slots
    // e poderiam saturar CPU. Mínimo real de 5 minutos é o razoável para qualquer serviço.
    // Mais importante: impede duracaoMinutos = 0, que causaria loop infinito em
    // gerarSlotsDisponiveis / gerarTodosSlots (cursor nunca avança).
    @Min(value = 5, message = "Duração mínima é 5 minutos")
    private Integer duracaoMinutos;

    @NotNull(message = "Preço obrigatório")
    @Positive(message = "Preço deve ser maior que zero")
    private BigDecimal preco;

    private String categoria;

    private BigDecimal custoMaterialFixo;

    private BigDecimal custoMaterialPercent;

    // GETTERS
    public String getNome()                        { return nome; }
    public String getDescricao()                   { return descricao; }
    public Integer getDuracaoMinutos()             { return duracaoMinutos; }
    public BigDecimal getPreco()                   { return preco; }
    public String getCategoria()                   { return categoria; }
    public BigDecimal getCustoMaterialFixo()       { return custoMaterialFixo; }
    public BigDecimal getCustoMaterialPercent()    { return custoMaterialPercent; }

    // SETTERS
    public void setNome(String nome)                                    { this.nome = nome; }
    public void setDescricao(String descricao)                          { this.descricao = descricao; }
    public void setDuracaoMinutos(Integer duracaoMinutos)               { this.duracaoMinutos = duracaoMinutos; }
    public void setPreco(BigDecimal preco)                              { this.preco = preco; }
    public void setCategoria(String categoria)                          { this.categoria = categoria; }
    public void setCustoMaterialFixo(BigDecimal custoMaterialFixo)      { this.custoMaterialFixo = custoMaterialFixo; }
    public void setCustoMaterialPercent(BigDecimal custoMaterialPercent){ this.custoMaterialPercent = custoMaterialPercent; }
}
