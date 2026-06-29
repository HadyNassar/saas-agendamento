package com.hady.saas.application.dto;

import com.hady.saas.domain.profissional.Profissional;

public class ProfissionalPublicoDTO {

    private Long id;
    private String nome;
    private String fotoPerfil;
    private String especialidade;
    private Double avaliacaoMedia;

    public ProfissionalPublicoDTO() {}

    public static ProfissionalPublicoDTO de(Profissional p) {
        return de(p, null);
    }

    public static ProfissionalPublicoDTO de(Profissional p, Double media) {
        ProfissionalPublicoDTO dto = new ProfissionalPublicoDTO();
        dto.id            = p.getId();
        dto.nome          = p.getNome();
        dto.fotoPerfil    = p.getFotoPerfil();
        dto.especialidade = p.getEspecialidades() != null && !p.getEspecialidades().isEmpty()
                ? p.getEspecialidades().get(0).getNome()
                : null;
        dto.avaliacaoMedia = media;
        return dto;
    }

    public Long getId()               { return id; }
    public String getNome()           { return nome; }
    public String getFotoPerfil()     { return fotoPerfil; }
    public String getEspecialidade()  { return especialidade; }
    public Double getAvaliacaoMedia() { return avaliacaoMedia; }

    public void setId(Long id)                    { this.id = id; }
    public void setNome(String nome)              { this.nome = nome; }
    public void setFotoPerfil(String foto)        { this.fotoPerfil = foto; }
    public void setEspecialidade(String esp)      { this.especialidade = esp; }
    public void setAvaliacaoMedia(Double media)   { this.avaliacaoMedia = media; }
}