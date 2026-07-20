package com.hady.saas.application.dto;

import java.math.BigDecimal;

// Versão ADMIN MASTER do pin do mapa — mesmo dado enxuto do
// EstabelecimentoMapaDTO público, porém com telefone/WhatsApp incluídos.
// Usado exclusivamente em /admin-master/mapa/estabelecimentos, protegido
// por hasRole("ADMIN_MASTER") no SecurityConfig. Nunca expor esse DTO
// em rota pública.
public class EstabelecimentoMapaAdminDTO {

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
    private boolean visivelNoMapa;
    private String telefone;
    private String whatsappContato;

    public EstabelecimentoMapaAdminDTO() {}

    public EstabelecimentoMapaAdminDTO(Long id, String nome, String slug, String tipo,
                                       String bairro, String cidade,
                                       BigDecimal latitude, BigDecimal longitude,
                                       String fotoPerfil, boolean aceitaAgendamentoOnline,
                                       boolean visivelNoMapa, String telefone, String whatsappContato) {
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
        this.visivelNoMapa = visivelNoMapa;
        this.telefone = telefone;
        this.whatsappContato = whatsappContato;
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

    public boolean isVisivelNoMapa()             { return visivelNoMapa; }
    public void setVisivelNoMapa(boolean v)      { this.visivelNoMapa = v; }

    public String getTelefone()                  { return telefone; }
    public void setTelefone(String telefone)     { this.telefone = telefone; }

    public String getWhatsappContato()                     { return whatsappContato; }
    public void setWhatsappContato(String whatsappContato) { this.whatsappContato = whatsappContato; }
}