package com.hady.saas.application.dto;

import java.math.BigDecimal;

// DTO enxuto usado na listagem pública do mapa.
// Contém só o necessário para desenhar um pin + card de preview —
// dados sensíveis (clientes, agendamentos, financeiro) nunca passam por aqui.
public class EstabelecimentoMapaDTO {

    private Long id;
    private String nome;
    private String slug;
    private String tipo;
    private String bairro;
    private String cidade;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String fotoPerfil;
    private boolean aceitaAgendamentoOnline;

    public EstabelecimentoMapaDTO() {}

    public EstabelecimentoMapaDTO(Long id, String nome, String slug, String tipo,
                                  String bairro, String cidade,
                                  BigDecimal latitude, BigDecimal longitude,
                                  String fotoPerfil, boolean aceitaAgendamentoOnline) {
        this.id = id;
        this.nome = nome;
        this.slug = slug;
        this.tipo = tipo;
        this.bairro = bairro;
        this.cidade = cidade;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fotoPerfil = fotoPerfil;
        this.aceitaAgendamentoOnline = aceitaAgendamentoOnline;
    }

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public String getNome()                      { return nome; }
    public void setNome(String nome)             { this.nome = nome; }

    public String getSlug()                      { return slug; }
    public void setSlug(String slug)             { this.slug = slug; }

    public String getTipo()                      { return tipo; }
    public void setTipo(String tipo)             { this.tipo = tipo; }

    public String getBairro()                    { return bairro; }
    public void setBairro(String bairro)         { this.bairro = bairro; }

    public String getCidade()                    { return cidade; }
    public void setCidade(String cidade)         { this.cidade = cidade; }

    public BigDecimal getLatitude()               { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude()               { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getFotoPerfil()                 { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil)  { this.fotoPerfil = fotoPerfil; }

    public boolean isAceitaAgendamentoOnline()               { return aceitaAgendamentoOnline; }
    public void setAceitaAgendamentoOnline(boolean a)        { this.aceitaAgendamentoOnline = a; }
}