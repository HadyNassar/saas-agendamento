package com.hady.saas.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CriarCupomDTO {

    // Se não vier preenchido, o service gera um código aleatório de 8 caracteres.
    private String codigo;

    @NotBlank(message = "Tipo de desconto obrigatório")
    private String tipoDesconto; // PERCENTUAL | VALOR_FIXO

    @NotNull(message = "Valor do desconto obrigatório")
    @DecimalMin(value = "0.01", message = "Valor do desconto deve ser maior que zero")
    private BigDecimal valorDesconto;

    private String planoRestrito; // opcional — null vale pra qualquer plano
    private LocalDate validadeData; // opcional
    private Integer limiteUsos; // opcional

    public String getCodigo()                          { return codigo; }
    public void setCodigo(String codigo)                { this.codigo = codigo; }

    public String getTipoDesconto()                     { return tipoDesconto; }
    public void setTipoDesconto(String v)               { this.tipoDesconto = v; }

    public BigDecimal getValorDesconto()                 { return valorDesconto; }
    public void setValorDesconto(BigDecimal v)           { this.valorDesconto = v; }

    public String getPlanoRestrito()                     { return planoRestrito; }
    public void setPlanoRestrito(String v)               { this.planoRestrito = v; }

    public LocalDate getValidadeData()                   { return validadeData; }
    public void setValidadeData(LocalDate v)             { this.validadeData = v; }

    public Integer getLimiteUsos()                        { return limiteUsos; }
    public void setLimiteUsos(Integer v)                  { this.limiteUsos = v; }
}