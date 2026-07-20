package com.hady.saas.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CupomDTO {

    private Long id;
    private String codigo;
    private String tipoDesconto;
    private BigDecimal valorDesconto;
    private String planoRestrito;
    private LocalDate validadeData;
    private Integer limiteUsos;
    private int usosAtuais;
    private boolean ativo;
    private boolean valido; // calculado — já considera data, limite e flag ativo
    private LocalDateTime criadoEm;

    public CupomDTO() {}

    public CupomDTO(Long id, String codigo, String tipoDesconto, BigDecimal valorDesconto,
                    String planoRestrito, LocalDate validadeData, Integer limiteUsos,
                    int usosAtuais, boolean ativo, boolean valido, LocalDateTime criadoEm) {
        this.id = id;
        this.codigo = codigo;
        this.tipoDesconto = tipoDesconto;
        this.valorDesconto = valorDesconto;
        this.planoRestrito = planoRestrito;
        this.validadeData = validadeData;
        this.limiteUsos = limiteUsos;
        this.usosAtuais = usosAtuais;
        this.ativo = ativo;
        this.valido = valido;
        this.criadoEm = criadoEm;
    }

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public String getCodigo()                        { return codigo; }
    public void setCodigo(String codigo)             { this.codigo = codigo; }

    public String getTipoDesconto()                   { return tipoDesconto; }
    public void setTipoDesconto(String v)             { this.tipoDesconto = v; }

    public BigDecimal getValorDesconto()               { return valorDesconto; }
    public void setValorDesconto(BigDecimal v)         { this.valorDesconto = v; }

    public String getPlanoRestrito()                   { return planoRestrito; }
    public void setPlanoRestrito(String v)             { this.planoRestrito = v; }

    public LocalDate getValidadeData()                 { return validadeData; }
    public void setValidadeData(LocalDate v)           { this.validadeData = v; }

    public Integer getLimiteUsos()                      { return limiteUsos; }
    public void setLimiteUsos(Integer v)                { this.limiteUsos = v; }

    public int getUsosAtuais()                          { return usosAtuais; }
    public void setUsosAtuais(int usosAtuais)           { this.usosAtuais = usosAtuais; }

    public boolean isAtivo()                            { return ativo; }
    public void setAtivo(boolean ativo)                 { this.ativo = ativo; }

    public boolean isValido()                            { return valido; }
    public void setValido(boolean valido)                { this.valido = valido; }

    public LocalDateTime getCriadoEm()                   { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm)      { this.criadoEm = criadoEm; }
}