package com.hady.saas.application.dto;

import com.hady.saas.domain.cupom.CupomEstabelecimento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CupomEstabelecimentoDTO {

    private final Long id;
    private final String codigo;
    private final String tipoDesconto;
    private final BigDecimal valorDesconto;
    private final LocalDate validadeData;
    private final Integer limiteUsos;
    private final int usosAtuais;
    private final boolean ativo;
    private final boolean valido;
    private final LocalDateTime criadoEm;

    public CupomEstabelecimentoDTO(CupomEstabelecimento cupom) {
        this.id = cupom.getId();
        this.codigo = cupom.getCodigo();
        this.tipoDesconto = cupom.getTipoDesconto();
        this.valorDesconto = cupom.getValorDesconto();
        this.validadeData = cupom.getValidadeData();
        this.limiteUsos = cupom.getLimiteUsos();
        this.usosAtuais = cupom.getUsosAtuais();
        this.ativo = cupom.isAtivo();
        this.valido = cupom.estaValido();
        this.criadoEm = cupom.getCriadoEm();
    }

    public Long getId()                       { return id; }
    public String getCodigo()                 { return codigo; }
    public String getTipoDesconto()           { return tipoDesconto; }
    public BigDecimal getValorDesconto()      { return valorDesconto; }
    public LocalDate getValidadeData()        { return validadeData; }
    public Integer getLimiteUsos()            { return limiteUsos; }
    public int getUsosAtuais()                 { return usosAtuais; }
    public boolean isAtivo()                   { return ativo; }
    public boolean isValido()                  { return valido; }
    public LocalDateTime getCriadoEm()         { return criadoEm; }
}