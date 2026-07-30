package com.hady.saas.application.dto;

import java.math.BigDecimal;

/**
 * Configuração do cupom que pode ser criado junto com uma campanha
 * (CriarCampanhaEnvioDTO.cupom). Um único cupom, compartilhado entre todos
 * os destinatários da campanha.
 *
 * diasValidade: quantos dias, a partir de hoje, o cupom fica ativo. Mesma
 * regra de negócio já usada em CupomEstabelecimentoService (máximo de 7
 * dias — decisão de produto, não limite técnico), validada no service.
 *
 * codigo: opcional. Se não informado, o service gera um código automático.
 *
 * limiteUsos: opcional. Se não informado, o cupom fica sem limite de usos
 * (qualquer destinatário pode usar quantas vezes quiser, dentro da
 * validade) — decisão do dono do estabelecimento por campanha.
 */
public class CupomCampanhaDTO {

    private String codigo;
    private String tipoDesconto; // PERCENTUAL | VALOR_FIXO
    private BigDecimal valorDesconto;
    private Integer diasValidade;
    private Integer limiteUsos;

    public CupomCampanhaDTO() {}

    public String getCodigo()             { return codigo; }
    public String getTipoDesconto()       { return tipoDesconto; }
    public BigDecimal getValorDesconto()  { return valorDesconto; }
    public Integer getDiasValidade()      { return diasValidade; }
    public Integer getLimiteUsos()        { return limiteUsos; }

    public void setCodigo(String codigo)                     { this.codigo = codigo; }
    public void setTipoDesconto(String tipoDesconto)         { this.tipoDesconto = tipoDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto)   { this.valorDesconto = valorDesconto; }
    public void setDiasValidade(Integer diasValidade)        { this.diasValidade = diasValidade; }
    public void setLimiteUsos(Integer limiteUsos)             { this.limiteUsos = limiteUsos; }
}
