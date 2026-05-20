package com.hady.saas.domain.servico;

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
    private String nome; // ex: Corte Masculino, Barba Completa

    private String descricao;

    @Column(nullable = false)
    private Integer duracaoMinutos; // duração do serviço em minutos

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    private String categoria; // ex: Cabelo, Barba, Estética, Unhas

    private boolean ativo = true;

    // Serviços pertencem ao estabelecimento (multi-tenant)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    // Construtor vazio obrigatório para JPA
    public Servico() {}

    // GETTERS
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public BigDecimal getPreco() { return preco; }
    public String getCategoria() { return categoria; }
    public boolean isAtivo() { return ativo; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public void setEstabelecimento(Estabelecimento estabelecimento) { this.estabelecimento = estabelecimento; }
}
