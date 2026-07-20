package com.hady.saas.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Uma linha da tabela de pagamentos/cobranças no painel financeiro.
public class CobrancaAdminDTO {

    private Long id;
    private Long estabelecimentoId;
    private String nomeEstabelecimento;
    private String plano;
    private String tipoCobranca;   // RECORRENTE | AVULSA
    private String status;         // AUTORIZADO | PENDENTE | RECUSADO | CANCELADO
    private BigDecimal valor;
    private LocalDate periodoInicio;
    private LocalDate periodoFim;
    private LocalDateTime criadoEm;

    public CobrancaAdminDTO() {}

    public CobrancaAdminDTO(Long id, Long estabelecimentoId, String nomeEstabelecimento,
                            String plano, String tipoCobranca, String status, BigDecimal valor,
                            LocalDate periodoInicio, LocalDate periodoFim, LocalDateTime criadoEm) {
        this.id = id;
        this.estabelecimentoId = estabelecimentoId;
        this.nomeEstabelecimento = nomeEstabelecimento;
        this.plano = plano;
        this.tipoCobranca = tipoCobranca;
        this.status = status;
        this.valor = valor;
        this.periodoInicio = periodoInicio;
        this.periodoFim = periodoFim;
        this.criadoEm = criadoEm;
    }

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public Long getEstabelecimentoId()                { return estabelecimentoId; }
    public void setEstabelecimentoId(Long v)          { this.estabelecimentoId = v; }

    public String getNomeEstabelecimento()                { return nomeEstabelecimento; }
    public void setNomeEstabelecimento(String v)          { this.nomeEstabelecimento = v; }

    public String getPlano()                          { return plano; }
    public void setPlano(String plano)                { this.plano = plano; }

    public String getTipoCobranca()                    { return tipoCobranca; }
    public void setTipoCobranca(String v)              { this.tipoCobranca = v; }

    public String getStatus()                          { return status; }
    public void setStatus(String status)               { this.status = status; }

    public BigDecimal getValor()                        { return valor; }
    public void setValor(BigDecimal valor)              { this.valor = valor; }

    public LocalDate getPeriodoInicio()                { return periodoInicio; }
    public void setPeriodoInicio(LocalDate v)          { this.periodoInicio = v; }

    public LocalDate getPeriodoFim()                    { return periodoFim; }
    public void setPeriodoFim(LocalDate v)              { this.periodoFim = v; }

    public LocalDateTime getCriadoEm()                  { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm)     { this.criadoEm = criadoEm; }
}