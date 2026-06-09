package com.hady.saas.domain.profissional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.domain.usuario.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profissionais")
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String telefone;
    private String fotoPerfil;

    @Column(name = "comissao_percent", precision = 5, scale = 2)
    private BigDecimal comissaoPercent = BigDecimal.ZERO;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "profissional_especialidades",
            joinColumns = @JoinColumn(name = "profissional_id"),
            inverseJoinColumns = @JoinColumn(name = "especialidade_id")
    )
    private List<Especialidade> especialidades = new ArrayList<>();

    /**
     * Relação N×N com Servico.
     * Um serviço (ex: "Corte de cabelo") pode estar vinculado a múltiplos
     * profissionais, e um profissional pode realizar múltiplos serviços.
     * Tabela intermediária: profissional_servicos
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "profissional_servicos",
            joinColumns = @JoinColumn(name = "profissional_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private List<Servico> servicos = new ArrayList<>();

    private boolean ativo = true;

    public Profissional() {}

    public Long getId()                            { return id; }
    public String getNome()                        { return nome; }
    public String getTelefone()                    { return telefone; }
    public String getFotoPerfil()                  { return fotoPerfil; }
    public BigDecimal getComissaoPercent()         { return comissaoPercent; }
    public Estabelecimento getEstabelecimento()    { return estabelecimento; }
    public Usuario getUsuario()                    { return usuario; }
    public List<Especialidade> getEspecialidades() { return especialidades; }
    public boolean isAtivo()                       { return ativo; }
    public List<Servico> getServicos()             { return servicos; }

    public void setId(Long id)                                          { this.id = id; }
    public void setNome(String nome)                                    { this.nome = nome; }
    public void setTelefone(String telefone)                            { this.telefone = telefone; }
    public void setFotoPerfil(String fotoPerfil)                        { this.fotoPerfil = fotoPerfil; }
    public void setComissaoPercent(BigDecimal comissaoPercent)          { this.comissaoPercent = comissaoPercent; }
    public void setEstabelecimento(Estabelecimento estabelecimento)     { this.estabelecimento = estabelecimento; }
    public void setUsuario(Usuario usuario)                             { this.usuario = usuario; }
    public void setEspecialidades(List<Especialidade> especialidades)   { this.especialidades = especialidades; }
    public void setAtivo(boolean ativo)                                 { this.ativo = ativo; }
    public void setServicos(List<Servico> servicos)                     { this.servicos = servicos; }
}
