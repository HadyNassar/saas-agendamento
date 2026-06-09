package com.hady.saas.application.dto;

import java.math.BigDecimal;

public class AtualizarComissaoDTO {

    private BigDecimal comissaoPercent;

    public BigDecimal getComissaoPercent() { return comissaoPercent; }
    public void setComissaoPercent(BigDecimal comissaoPercent) { this.comissaoPercent = comissaoPercent; }
}
