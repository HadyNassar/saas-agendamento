package com.hady.saas.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// Resumo financeiro completo — cartões principais + distribuição por plano
// + projeção de MRR pros próximos meses.
public class ResumoFinanceiroDTO {

    private BigDecimal receitaTotalPeriodo;
    private BigDecimal mrrAtual;
    private long totalInadimplentes;
    private BigDecimal valorEmAtraso;

    private Map<String, BigDecimal> receitaPorPlano;
    private List<PontoSerieDTO> projecaoMrr3Meses;

    public ResumoFinanceiroDTO() {}

    public BigDecimal getReceitaTotalPeriodo()               { return receitaTotalPeriodo; }
    public void setReceitaTotalPeriodo(BigDecimal v)         { this.receitaTotalPeriodo = v; }

    public BigDecimal getMrrAtual()                          { return mrrAtual; }
    public void setMrrAtual(BigDecimal v)                    { this.mrrAtual = v; }

    public long getTotalInadimplentes()                       { return totalInadimplentes; }
    public void setTotalInadimplentes(long v)                 { this.totalInadimplentes = v; }

    public BigDecimal getValorEmAtraso()                      { return valorEmAtraso; }
    public void setValorEmAtraso(BigDecimal v)                { this.valorEmAtraso = v; }

    public Map<String, BigDecimal> getReceitaPorPlano()      { return receitaPorPlano; }
    public void setReceitaPorPlano(Map<String, BigDecimal> v) { this.receitaPorPlano = v; }

    public List<PontoSerieDTO> getProjecaoMrr3Meses()        { return projecaoMrr3Meses; }
    public void setProjecaoMrr3Meses(List<PontoSerieDTO> v)  { this.projecaoMrr3Meses = v; }
}