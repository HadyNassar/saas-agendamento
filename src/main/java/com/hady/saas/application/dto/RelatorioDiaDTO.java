package com.hady.saas.application.dto;

public class RelatorioDiaDTO {

    private String data;
    private double faturamento;
    private double lucro;

    public RelatorioDiaDTO(String data, double faturamento, double lucro) {
        this.data = data;
        this.faturamento = faturamento;
        this.lucro = lucro;
    }

    public String getData()        { return data; }
    public double getFaturamento() { return faturamento; }
    public double getLucro()       { return lucro; }
}
