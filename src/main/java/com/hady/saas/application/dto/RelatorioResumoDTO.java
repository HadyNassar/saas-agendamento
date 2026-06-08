package com.hady.saas.application.dto;

public class RelatorioResumoDTO {

    private double faturamentoBruto;
    private double totalCustos;
    private double lucroLiquido;
    private double margemLucro;
    private double ticketMedio;
    private int totalAtendimentos;
    private double totalComissoes;
    private double totalCustoMaterial;

    public RelatorioResumoDTO(double faturamentoBruto, double totalCustos, double lucroLiquido,
                              double margemLucro, double ticketMedio, int totalAtendimentos,
                              double totalComissoes, double totalCustoMaterial) {
        this.faturamentoBruto = faturamentoBruto;
        this.totalCustos = totalCustos;
        this.lucroLiquido = lucroLiquido;
        this.margemLucro = margemLucro;
        this.ticketMedio = ticketMedio;
        this.totalAtendimentos = totalAtendimentos;
        this.totalComissoes = totalComissoes;
        this.totalCustoMaterial = totalCustoMaterial;
    }

    public double getFaturamentoBruto()    { return faturamentoBruto; }
    public double getTotalCustos()         { return totalCustos; }
    public double getLucroLiquido()        { return lucroLiquido; }
    public double getMargemLucro()         { return margemLucro; }
    public double getTicketMedio()         { return ticketMedio; }
    public int getTotalAtendimentos()      { return totalAtendimentos; }
    public double getTotalComissoes()      { return totalComissoes; }
    public double getTotalCustoMaterial()  { return totalCustoMaterial; }
}
