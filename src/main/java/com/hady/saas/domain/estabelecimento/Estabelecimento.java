package com.hady.saas.domain.estabelecimento;

import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.enums.TipoEstabelecimento;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "estabelecimentos")
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, nullable = false)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEstabelecimento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plano plano;

    private String telefone;
    private String endereco;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    private boolean ativo = true;

    // WhatsApp / Evolution API
    @Column(unique = true)
    private String evolutionInstanceName;

    private boolean whatsappConectado = false;

    // Quantos meses à frente o cliente pode agendar (1, 3, 6 ou 12)
    @Column(name = "meses_antecipacao", nullable = false)
    private Integer mesesAntecipacao = 3;

    public Estabelecimento() {}

    public boolean podeCadastrarUsuario(int totalUsuariosAtual) {
        return totalUsuariosAtual < plano.getLimiteUsuarios();
    }

    // ── GETTERS ──────────────────────────────────────────────────────────────
    public Long getId()                        { return id; }
    public String getNome()                    { return nome; }
    public String getSlug()                    { return slug; }
    public TipoEstabelecimento getTipo()       { return tipo; }
    public Plano getPlano()                    { return plano; }
    public String getTelefone()                { return telefone; }
    public String getEndereco()                { return endereco; }
    public LocalDateTime getCriadoEm()         { return criadoEm; }
    public boolean isAtivo()                   { return ativo; }
    public String getEvolutionInstanceName()   { return evolutionInstanceName; }
    public boolean isWhatsappConectado()       { return whatsappConectado; }
    public Integer getMesesAntecipacao()       { return mesesAntecipacao; }

    // ── SETTERS ──────────────────────────────────────────────────────────────
    public void setId(Long id)                                         { this.id = id; }
    public void setNome(String nome)                                   { this.nome = nome; }
    public void setSlug(String slug)                                   { this.slug = slug; }
    public void setTipo(TipoEstabelecimento tipo)                      { this.tipo = tipo; }
    public void setPlano(Plano plano)                                  { this.plano = plano; }
    public void setTelefone(String telefone)                           { this.telefone = telefone; }
    public void setEndereco(String endereco)                           { this.endereco = endereco; }
    public void setCriadoEm(LocalDateTime criadoEm)                   { this.criadoEm = criadoEm; }
    public void setAtivo(boolean ativo)                                { this.ativo = ativo; }
    public void setEvolutionInstanceName(String evolutionInstanceName) { this.evolutionInstanceName = evolutionInstanceName; }
    public void setWhatsappConectado(boolean whatsappConectado)        { this.whatsappConectado = whatsappConectado; }
    public void setMesesAntecipacao(Integer mesesAntecipacao)          { this.mesesAntecipacao = mesesAntecipacao; }
}
