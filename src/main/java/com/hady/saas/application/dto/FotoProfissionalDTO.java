package com.hady.saas.application.dto;

import com.hady.saas.domain.fotoprofissional.FotoProfissional;

import java.time.LocalDateTime;

public class FotoProfissionalDTO {

    private Long id;
    private String url;
    private LocalDateTime criadoEm;

    public static FotoProfissionalDTO from(FotoProfissional foto, String baseUrl) {
        FotoProfissionalDTO dto = new FotoProfissionalDTO();
        dto.setId(foto.getId());
        dto.setUrl(baseUrl + "/uploads/profissionais/" + foto.getProfissional().getId()
                + "/" + extrairNomeArquivo(foto.getCaminho()));
        dto.setCriadoEm(foto.getCriadoEm());
        return dto;
    }

    private static String extrairNomeArquivo(String caminho) {
        return caminho.contains("/")
                ? caminho.substring(caminho.lastIndexOf("/") + 1)
                : caminho;
    }

    public Long getId() { return id; }
    public String getUrl() { return url; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public void setId(Long id) { this.id = id; }
    public void setUrl(String url) { this.url = url; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
