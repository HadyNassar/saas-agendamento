package com.hady.saas.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CriarCupomEstabelecimentoDTO {

    private String codigo;
    private String tipoDesconto; // PERCENTUAL | VALOR_FIXO
    private BigDecimal valorDesconto;
    private LocalDate validadeData;   // opcional — nulo = sem expiração
    private Integer limiteUsos;       // opcional — nulo = ilimitado

    public CriarCupomEstabelecimentoDTO() {}

    public String getCodigo()                { return codigo; }
    public String getTipoDesconto()          { return tipoDesconto; }
    public BigDecimal getValorDesconto()     { return valorDesconto; }
    public LocalDate getValidadeData()       { return validadeData; }
    public Integer getLimiteUsos()           { return limiteUsos; }

    public void setCodigo(String codigo)                       { this.codigo = codigo; }
    public void setTipoDesconto(String tipoDesconto)           { this.tipoDesconto = tipoDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto)     { this.valorDesconto = valorDesconto; }
    public void setValidadeData(LocalDate validadeData)        { this.validadeData = validadeData; }
    public void setLimiteUsos(Integer limiteUsos)               { this.limiteUsos = limiteUsos; }
}