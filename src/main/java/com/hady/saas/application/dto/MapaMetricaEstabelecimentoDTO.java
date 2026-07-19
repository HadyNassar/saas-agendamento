package com.hady.saas.application.dto;

// Uma linha da tela "Gestão do Mapa" — um estabelecimento cadastrado com
// suas métricas de visualização e visibilidade atual no mapa público.
public class MapaMetricaEstabelecimentoDTO {

    private Long id;
    private String nome;
    private String slug;
    private String cidade;
    private String bairro;
    private boolean visivelNoMapa;
    private boolean ativo;
    private long visualizacoesTotal;
    private long visualizacoes30Dias;
    private long visualizacoesHoje;

    public MapaMetricaEstabelecimentoDTO() {}

    public MapaMetricaEstabelecimentoDTO(Long id, String nome, String slug, String cidade, String bairro,
                                         boolean visivelNoMapa, boolean ativo,
                                         long visualizacoesTotal, long visualizacoes30Dias, long visualizacoesHoje) {
        this.id = id;
        this.nome = nome;
        this.slug = slug;
        this.cidade = cidade;
        this.bairro = bairro;
        this.visivelNoMapa = visivelNoMapa;
        this.ativo = ativo;
        this.visualizacoesTotal = visualizacoesTotal;
        this.visualizacoes30Dias = visualizacoes30Dias;
        this.visualizacoesHoje = visualizacoesHoje;
    }

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public String getNome()                      { return nome; }
    public void setNome(String nome)             { this.nome = nome; }

    public String getSlug()                      { return slug; }
    public void setSlug(String slug)             { this.slug = slug; }

    public String getCidade()                    { return cidade; }
    public void setCidade(String cidade)         { this.cidade = cidade; }

    public String getBairro()                    { return bairro; }
    public void setBairro(String bairro)         { this.bairro = bairro; }

    public boolean isVisivelNoMapa()             { return visivelNoMapa; }
    public void setVisivelNoMapa(boolean v)      { this.visivelNoMapa = v; }

    public boolean isAtivo()                     { return ativo; }
    public void setAtivo(boolean ativo)          { this.ativo = ativo; }

    public long getVisualizacoesTotal()               { return visualizacoesTotal; }
    public void setVisualizacoesTotal(long v)         { this.visualizacoesTotal = v; }

    public long getVisualizacoes30Dias()               { return visualizacoes30Dias; }
    public void setVisualizacoes30Dias(long v)         { this.visualizacoes30Dias = v; }

    public long getVisualizacoesHoje()                 { return visualizacoesHoje; }
    public void setVisualizacoesHoje(long v)           { this.visualizacoesHoje = v; }
}