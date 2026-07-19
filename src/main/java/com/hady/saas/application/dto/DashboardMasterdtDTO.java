package com.hady.saas.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// Visão consolidada de TODOS os estabelecimentos do SaaS — consumida
// pelo painel admin master. Nunca expõe dados sensíveis de cliente
// final (nome/telefone de pacientes, etc), só métricas agregadas.
public class DashboardMasterDTO {

    // ── Cartões principais ──────────────────────────────────────────────
    private long totalEstabelecimentos;
    private long estabelecimentosAtivos;
    private long estabelecimentosEmTrial;
    private long estabelecimentosInativos;
    private long novosHoje;
    private long novosSemana;
    private long novosMes;

    private BigDecimal mrrAtual;           // soma de cobranças RECORRENTE ativas do mês
    private BigDecimal receitaMesAtual;    // soma de tudo pago (recorrente + avulso) no mês
    private double churnMesAtual;          // % de cancelados no mês / ativos no início do mês

    // ── Distribuição por plano (pizza) ──────────────────────────────────
    private Map<String, Long> distribuicaoPorPlano;

    // ── Trial vs Pagante ─────────────────────────────────────────────────
    private long totalTrial;
    private long totalPagante;

    // ── Séries temporais (linha/barra) — últimos 6 meses ────────────────
    private List<PontoSerieDTO> crescimentoUsuariosPorMes;
    private List<PontoSerieDTO> receitaPorMes;

    // ── Mapa público ──────────────────────────────────────────────────
    private long visualizacoesMapaHoje;
    private long leadsGeradosTotal;

    public DashboardMasterDTO() {}

    // GETTERS
    public long getTotalEstabelecimentos()             { return totalEstabelecimentos; }
    public long getEstabelecimentosAtivos()             { return estabelecimentosAtivos; }
    public long getEstabelecimentosEmTrial()            { return estabelecimentosEmTrial; }
    public long getEstabelecimentosInativos()           { return estabelecimentosInativos; }
    public long getNovosHoje()                          { return novosHoje; }
    public long getNovosSemana()                        { return novosSemana; }
    public long getNovosMes()                           { return novosMes; }
    public BigDecimal getMrrAtual()                     { return mrrAtual; }
    public BigDecimal getReceitaMesAtual()               { return receitaMesAtual; }
    public double getChurnMesAtual()                    { return churnMesAtual; }
    public Map<String, Long> getDistribuicaoPorPlano()  { return distribuicaoPorPlano; }
    public long getTotalTrial()                          { return totalTrial; }
    public long getTotalPagante()                        { return totalPagante; }
    public List<PontoSerieDTO> getCrescimentoUsuariosPorMes() { return crescimentoUsuariosPorMes; }
    public List<PontoSerieDTO> getReceitaPorMes()        { return receitaPorMes; }
    public long getVisualizacoesMapaHoje()               { return visualizacoesMapaHoje; }
    public long getLeadsGeradosTotal()                   { return leadsGeradosTotal; }

    // SETTERS
    public void setTotalEstabelecimentos(long v)             { this.totalEstabelecimentos = v; }
    public void setEstabelecimentosAtivos(long v)             { this.estabelecimentosAtivos = v; }
    public void setEstabelecimentosEmTrial(long v)            { this.estabelecimentosEmTrial = v; }
    public void setEstabelecimentosInativos(long v)           { this.estabelecimentosInativos = v; }
    public void setNovosHoje(long v)                          { this.novosHoje = v; }
    public void setNovosSemana(long v)                        { this.novosSemana = v; }
    public void setNovosMes(long v)                           { this.novosMes = v; }
    public void setMrrAtual(BigDecimal v)                     { this.mrrAtual = v; }
    public void setReceitaMesAtual(BigDecimal v)               { this.receitaMesAtual = v; }
    public void setChurnMesAtual(double v)                    { this.churnMesAtual = v; }
    public void setDistribuicaoPorPlano(Map<String, Long> v)  { this.distribuicaoPorPlano = v; }
    public void setTotalTrial(long v)                          { this.totalTrial = v; }
    public void setTotalPagante(long v)                        { this.totalPagante = v; }
    public void setCrescimentoUsuariosPorMes(List<PontoSerieDTO> v) { this.crescimentoUsuariosPorMes = v; }
    public void setReceitaPorMes(List<PontoSerieDTO> v)       { this.receitaPorMes = v; }
    public void setVisualizacoesMapaHoje(long v)               { this.visualizacoesMapaHoje = v; }
    public void setLeadsGeradosTotal(long v)                   { this.leadsGeradosTotal = v; }
}