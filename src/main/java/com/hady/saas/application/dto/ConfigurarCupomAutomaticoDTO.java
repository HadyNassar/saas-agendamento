package com.hady.saas.application.dto;

import java.math.BigDecimal;

/**
 * Corpo de PUT /painel/whatsapp/cupom-automatico — liga/desliga e configura
 * o cupom automático de reativação (item 8). `ativo=false` ignora
 * tipoDesconto/valorDesconto (podem vir nulos nesse caso).
 */
public class ConfigurarCupomAutomaticoDTO {

    private boolean ativo;
    private String tipoDesconto; // PERCENTUAL | VALOR_FIXO — obrigatório se ativo=true
    private BigDecimal valorDesconto; // obrigatório se ativo=true

    public ConfigurarCupomAutomaticoDTO() {}

    public boolean isAtivo()                      { return ativo; }
    public String getTipoDesconto()               { return tipoDesconto; }
    public BigDecimal getValorDesconto()           { return valorDesconto; }

    public void setAtivo(boolean ativo)                        { this.ativo = ativo; }
    public void setTipoDesconto(String tipoDesconto)            { this.tipoDesconto = tipoDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto)      { this.valorDesconto = valorDesconto; }
}