package com.hady.saas.application.dto;

public class RelatorioServicoDTO {

    private String nome;
    private int atendimentos;
    private double faturamento;
    private double custoMaterial;
    private double comissoes;
    private double lucro;
    private double margem;

    public RelatorioServicoDTO(String nome) {
        this.nome = nome;
    }

    public void incrementar(double preco, double custoMaterial, double comissao) {
        this.atendimentos++;
        this.faturamento   += preco;
        this.custoMaterial += custoMaterial;
        this.comissoes     += comissao;
        this.lucro         += preco - custoMaterial - comissao;
    }

    public void calcularMargem() {
        this.margem        = faturamento > 0 ? (lucro / faturamento) * 100 : 0;
        this.faturamento   = round(faturamento);
        this.custoMaterial = round(custoMaterial);
        this.comissoes     = round(comissoes);
        this.lucro         = round(lucro);
        this.margem        = round(margem);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public String getNome()          { return nome; }
    public int getAtendimentos()     { return atendimentos; }
    public double getFaturamento()   { return faturamento; }
    public double getCustoMaterial() { return custoMaterial; }
    public double getComissoes()     { return comissoes; }
    public double getLucro()         { return lucro; }
    public double getMargem()        { return margem; }
}
