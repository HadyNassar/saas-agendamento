package com.hady.saas.application.dto;

import com.hady.saas.domain.profissional.Profissional;

public class ProfissionalPublicoDTO {

    private Long id;
    private String nome;
    private String fotoPerfil;

    public ProfissionalPublicoDTO() {}

    public static ProfissionalPublicoDTO de(Profissional p) {
        ProfissionalPublicoDTO dto = new ProfissionalPublicoDTO();
        dto.id = p.getId();
        dto.nome = p.getNome();
        dto.fotoPerfil = p.getFotoPerfil();
        return dto;
    }

    public Long getId()         { return id; }
    public String getNome()     { return nome; }
    public String getFotoPerfil() { return fotoPerfil; }

    public void setId(Long id)              { this.id = id; }
    public void setNome(String nome)        { this.nome = nome; }
    public void setFotoPerfil(String foto)  { this.fotoPerfil = foto; }
}