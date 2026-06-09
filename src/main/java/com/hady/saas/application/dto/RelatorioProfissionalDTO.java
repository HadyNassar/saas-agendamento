package com.hady.saas.application.dto;

public class RelatorioProfissionalDTO {

    private String nome;
    private int atendimentos;
    private double faturamento;
    private double comissao;
    private double custoMaterial;
    private double lucroGerado;
    private double margem;

    public RelatorioProfissionalDTO(String nome) {
        this.nome = nome;
    }

    public void incrementar(double preco, double custoMaterial, double comissao) {
        this.atendimentos++;
        this.faturamento   += preco;
        this.custoMaterial += custoMaterial;
        this.comissao      += comissao;
        this.lucroGerado   += preco - custoMaterial - comissao;
    }

    public void calcularMargem() {
        this.margem        = faturamento > 0 ? (lucroGerado / faturamento) * 100 : 0;
        this.faturamento   = round(faturamento);
        this.custoMaterial = round(custoMaterial);
        this.comissao      = round(comissao);
        this.lucroGerado   = round(lucroGerado);
        this.margem        = round(margem);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public String getNome()          { return nome; }
    public int getAtendimentos()     { return atendimentos; }
    public double getFaturamento()   { return faturamento; }
    public double getComissao()      { return comissao; }
    public double getCustoMaterial() { return custoMaterial; }
    public double getLucroGerado()   { return lucroGerado; }
    public double getMargem()        { return margem; }
}
