package com.hady.saas.domain.cliente;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String telefone;

    private String email;
    private String alergias;
    private String preferencias;
    private String restricoes;
    private String observacoes;
    private int totalFaltas = 0;
    private boolean bloqueado = false;
    private String motivoBloqueio;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    public Cliente() {}

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public String getAlergias() { return alergias; }
    public String getPreferencias() { return preferencias; }
    public String getRestricoes() { return restricoes; }
    public String getObservacoes() { return observacoes; }
    public int getTotalFaltas() { return totalFaltas; }
    public boolean isBloqueado() { return bloqueado; }
    public String getMotivoBloqueio() { return motivoBloqueio; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }

    public void setId(Long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setEmail(String email) { this.email = email; }
    public void setAlergias(String alergias) { this.alergias = alergias; }
    public void setPreferencias(String preferencias) { this.preferencias = preferencias; }
    public void setRestricoes(String restricoes) { this.restricoes = restricoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public void setTotalFaltas(int totalFaltas) { this.totalFaltas = totalFaltas; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public void setMotivoBloqueio(String motivoBloqueio) { this.motivoBloqueio = motivoBloqueio; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public void setEstabelecimento(Estabelecimento estabelecimento) { this.estabelecimento = estabelecimento; }
}