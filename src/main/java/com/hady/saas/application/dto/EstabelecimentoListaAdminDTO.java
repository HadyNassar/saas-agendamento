package com.hady.saas.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Linha da tabela de gestão de usuários (admin master) — dados suficientes
// para listar, filtrar e ordenar sem carregar tudo (fotos, comodidades etc).
// O detalhe completo fica em EstabelecimentoAdminDTO, buscado sob demanda.
public class EstabelecimentoListaAdminDTO {

    private Long id;
    private String nome;
    private String slug;
    private String tipo;
    private String cidade;
    private String estado;
    private String plano;
    private String status; // ATIVO | TRIAL | INATIVO
    private LocalDateTime criadoEm;
    private LocalDate planoValidoAte;
    private LocalDate trialFim;
    private boolean ativo;

    public EstabelecimentoListaAdminDTO() {}

    public EstabelecimentoListaAdminDTO(Long id, String nome, String slug, String tipo,
                                        String cidade, String estado, String plano, String status,
                                        LocalDateTime criadoEm, LocalDate planoValidoAte,
                                        LocalDate trialFim, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.slug = slug;
        this.tipo = tipo;
        this.cidade = cidade;
        this.estado = estado;
        this.plano = plano;
        this.status = status;
        this.criadoEm = criadoEm;
        this.planoValidoAte = planoValidoAte;
        this.trialFim = trialFim;
        this.ativo = ativo;
    }

    public Long getId()                            { return id; }
    public void setId(Long id)                     { this.id = id; }

    public String getNome()                        { return nome; }
    public void setNome(String nome)               { this.nome = nome; }

    public String getSlug()                        { return slug; }
    public void setSlug(String slug)               { this.slug = slug; }

    public String getTipo()                        { return tipo; }
    public void setTipo(String tipo)               { this.tipo = tipo; }

    public String getCidade()                      { return cidade; }
    public void setCidade(String cidade)           { this.cidade = cidade; }

    public String getEstado()                      { return estado; }
    public void setEstado(String estado)           { this.estado = estado; }

    public String getPlano()                       { return plano; }
    public void setPlano(String plano)             { this.plano = plano; }

    public String getStatus()                      { return status; }
    public void setStatus(String status)           { this.status = status; }

    public LocalDateTime getCriadoEm()             { return criadoEm; }
    public void setCriadoEm(LocalDateTime v)       { this.criadoEm = v; }

    public LocalDate getPlanoValidoAte()           { return planoValidoAte; }
    public void setPlanoValidoAte(LocalDate v)     { this.planoValidoAte = v; }

    public LocalDate getTrialFim()                 { return trialFim; }
    public void setTrialFim(LocalDate v)           { this.trialFim = v; }

    public boolean isAtivo()                       { return ativo; }
    public void setAtivo(boolean ativo)            { this.ativo = ativo; }
}