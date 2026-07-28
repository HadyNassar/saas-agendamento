package com.hady.saas.domain.cliente;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hady.saas.domain.enums.EstagioCliente;
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

    // Controla quando foi enviado o último lembrete de retorno (evita spam)
    @Column(name = "ultimo_lembrete_retorno")
    private LocalDateTime ultimoLembreteRetorno;

    // Migration V23 — snapshot persistido do EstagioCliente calculado pelo
    // ClienteSegmentacaoService. O cálculo sob demanda (calcular()) continua
    // sendo a fonte da verdade para leituras pontuais; estas duas colunas
    // existem só para permitir consultas em massa (ex: "todos os VIP") sem
    // recalcular cliente por cliente, e para os itens 10/11 saberem QUANDO a
    // mudança aconteceu (ver ClienteEstagioHistorico). Pode ficar nulo até a
    // primeira chamada de recalcularEPersistir() para o cliente.
    @Enumerated(EnumType.STRING)
    @Column(name = "estagio_atual")
    private EstagioCliente estagioAtual;

    @Column(name = "estagio_atualizado_em")
    private LocalDateTime estagioAtualizadoEm;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    public Cliente() {}

    public Long getId()                          { return id; }
    public String getNome()                      { return nome; }
    public String getTelefone()                  { return telefone; }
    public String getEmail()                     { return email; }
    public String getAlergias()                  { return alergias; }
    public String getPreferencias()              { return preferencias; }
    public String getRestricoes()                { return restricoes; }
    public String getObservacoes()               { return observacoes; }
    public int getTotalFaltas()                  { return totalFaltas; }
    public boolean isBloqueado()                 { return bloqueado; }
    public String getMotivoBloqueio()            { return motivoBloqueio; }
    public LocalDateTime getUltimoLembreteRetorno() { return ultimoLembreteRetorno; }
    public LocalDateTime getCriadoEm()           { return criadoEm; }
    public Estabelecimento getEstabelecimento()  { return estabelecimento; }
    public EstagioCliente getEstagioAtual()      { return estagioAtual; }
    public LocalDateTime getEstagioAtualizadoEm(){ return estagioAtualizadoEm; }

    public void setId(Long id)                                     { this.id = id; }
    public void setNome(String nome)                               { this.nome = nome; }
    public void setTelefone(String telefone)                       { this.telefone = telefone; }
    public void setEmail(String email)                             { this.email = email; }
    public void setAlergias(String alergias)                       { this.alergias = alergias; }
    public void setPreferencias(String preferencias)               { this.preferencias = preferencias; }
    public void setRestricoes(String restricoes)                   { this.restricoes = restricoes; }
    public void setObservacoes(String observacoes)                 { this.observacoes = observacoes; }
    public void setTotalFaltas(int totalFaltas)                    { this.totalFaltas = totalFaltas; }
    public void setBloqueado(boolean bloqueado)                    { this.bloqueado = bloqueado; }
    public void setMotivoBloqueio(String motivoBloqueio)           { this.motivoBloqueio = motivoBloqueio; }
    public void setUltimoLembreteRetorno(LocalDateTime dt)         { this.ultimoLembreteRetorno = dt; }
    public void setCriadoEm(LocalDateTime criadoEm)               { this.criadoEm = criadoEm; }
    public void setEstabelecimento(Estabelecimento estabelecimento){ this.estabelecimento = estabelecimento; }
    public void setEstagioAtual(EstagioCliente estagioAtual)       { this.estagioAtual = estagioAtual; }
    public void setEstagioAtualizadoEm(LocalDateTime dt)           { this.estagioAtualizadoEm = dt; }
}