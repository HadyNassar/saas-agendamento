package com.hady.saas.application.dto;

public class EstabelecimentoResponseDTO {

    private Long id;
    private String nome;
    private String slug;
    private String telefone;
    private String endereco;
    private Integer mesesAntecipacao;
    private boolean whatsappConectado;

    public EstabelecimentoResponseDTO() {}

    public EstabelecimentoResponseDTO(Long id, String nome, String slug,
                                       String telefone, String endereco,
                                       Integer mesesAntecipacao, boolean whatsappConectado) {
        this.id = id;
        this.nome = nome;
        this.slug = slug;
        this.telefone = telefone;
        this.endereco = endereco;
        this.mesesAntecipacao = mesesAntecipacao;
        this.whatsappConectado = whatsappConectado;
    }

    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }

    public String getNome()                { return nome; }
    public void setNome(String nome)       { this.nome = nome; }

    public String getSlug()                { return slug; }
    public void setSlug(String slug)       { this.slug = slug; }

    public String getTelefone()            { return telefone; }
    public void setTelefone(String t)      { this.telefone = t; }

    public String getEndereco()            { return endereco; }
    public void setEndereco(String e)      { this.endereco = e; }

    public Integer getMesesAntecipacao()           { return mesesAntecipacao; }
    public void setMesesAntecipacao(Integer m)     { this.mesesAntecipacao = m; }

    public boolean isWhatsappConectado()           { return whatsappConectado; }
    public void setWhatsappConectado(boolean w)    { this.whatsappConectado = w; }
}
