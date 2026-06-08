package com.hady.saas.application.dto;

import java.util.List;

public class RelatorioFinanceiroDTO {

    private RelatorioResumoDTO resumo;
    private List<RelatorioDiaDTO> porDia;
    private List<RelatorioServicoDTO> servicos;
    private List<RelatorioProfissionalDTO> profissionais;

    public RelatorioFinanceiroDTO(RelatorioResumoDTO resumo,
                                  List<RelatorioDiaDTO> porDia,
                                  List<RelatorioServicoDTO> servicos,
                                  List<RelatorioProfissionalDTO> profissionais) {
        this.resumo = resumo;
        this.porDia = porDia;
        this.servicos = servicos;
        this.profissionais = profissionais;
    }

    public RelatorioResumoDTO getResumo()                    { return resumo; }
    public List<RelatorioDiaDTO> getPorDia()                 { return porDia; }
    public List<RelatorioServicoDTO> getServicos()           { return servicos; }
    public List<RelatorioProfissionalDTO> getProfissionais() { return profissionais; }
}