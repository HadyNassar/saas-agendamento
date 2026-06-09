package com.hady.saas.domain.servico;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "servicos")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(nullable = false)
    private Integer duracaoMinutos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    private String categoria;

    private boolean ativo = true;

    @Column(name = "custo_material_fixo", precision = 10, scale = 2)
    private BigDecimal custoMaterialFixo = BigDecimal.ZERO;

    @Column(name = "custo_material_percent", precision = 5, scale = 2)
    private BigDecimal custoMaterialPercent = BigDecimal.ZERO;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    public Servico() {}

    public Long getId()                        { return id; }
    public String getNome()                    { return nome; }
    public String getDescricao()               { return descricao; }
    public Integer getDuracaoMinutos()         { return duracaoMinutos; }
    public BigDecimal getPreco()               { return preco; }
    public String getCategoria()               { return categoria; }
    public boolean isAtivo()                   { return ativo; }
    public BigDecimal getCustoMaterialFixo()   { return custoMaterialFixo; }
    public BigDecimal getCustoMaterialPercent(){ return custoMaterialPercent; }
    public Estabelecimento getEstabelecimento(){ return estabelecimento; }

    public void setId(Long id)                                       { this.id = id; }
    public void setNome(String nome)                                 { this.nome = nome; }
    public void setDescricao(String descricao)                       { this.descricao = descricao; }
    public void setDuracaoMinutos(Integer duracaoMinutos)            { this.duracaoMinutos = duracaoMinutos; }
    public void setPreco(BigDecimal preco)                           { this.preco = preco; }
    public void setCategoria(String categoria)                       { this.categoria = categoria; }
    public void setAtivo(boolean ativo)                              { this.ativo = ativo; }
    public void setCustoMaterialFixo(BigDecimal custoMaterialFixo)   { this.custoMaterialFixo = custoMaterialFixo; }
    public void setCustoMaterialPercent(BigDecimal v)                { this.custoMaterialPercent = v; }
    public void setEstabelecimento(Estabelecimento estabelecimento)  { this.estabelecimento = estabelecimento; }
}
