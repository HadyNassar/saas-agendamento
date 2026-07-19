package com.hady.saas.application.dto;

import java.math.BigDecimal;

// Ponto genérico de série temporal — usado em qualquer gráfico de linha
// ou barra do dashboard (crescimento de usuários, receita por mês, etc).
public class PontoSerieDTO {

    private String label;       // ex: "Jan/2026", "Fev/2026"
    private long valorInteiro;  // usado para contagens (novos usuários, etc)
    private BigDecimal valorDecimal; // usado para valores monetários

    public PontoSerieDTO() {}

    public PontoSerieDTO(String label, long valorInteiro) {
        this.label = label;
        this.valorInteiro = valorInteiro;
    }

    public PontoSerieDTO(String label, BigDecimal valorDecimal) {
        this.label = label;
        this.valorDecimal = valorDecimal;
    }

    public String getLabel()                        { return label; }
    public void setLabel(String label)              { this.label = label; }

    public long getValorInteiro()                    { return valorInteiro; }
    public void setValorInteiro(long valorInteiro)   { this.valorInteiro = valorInteiro; }

    public BigDecimal getValorDecimal()                     { return valorDecimal; }
    public void setValorDecimal(BigDecimal valorDecimal)   { this.valorDecimal = valorDecimal; }
}