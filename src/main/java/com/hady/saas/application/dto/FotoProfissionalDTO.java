package com.hady.saas.application.dto;

import com.hady.saas.domain.fotoprofissional.FotoProfissional;

import java.time.LocalDateTime;

public class FotoProfissionalDTO {

    private Long id;
    private String url;
    private LocalDateTime criadoEm;

    public FotoProfissionalDTO() {}

    /**
     * Usado pelo FotoPainelController (painel autenticado).
     * URL aponta para /painel/fotos/profissionais/{id}/serve — requer JWT.
     *
     * Para portfólio público, o FotoPublicoController constrói o DTO manualmente
     * com a URL pré-assinada gerada por FotoService.gerarUrlPublicaProfissional().
     */
    public static FotoProfissionalDTO from(FotoProfissional foto, String baseUrl) {
        FotoProfissionalDTO dto = new FotoProfissionalDTO();
        dto.setId(foto.getId());
        dto.setUrl(baseUrl + "/painel/fotos/profissionais/" + foto.getId() + "/serve");
        dto.setCriadoEm(foto.getCriadoEm());
        return dto;
    }

    public Long getId() { return id; }
    public String getUrl() { return url; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public void setId(Long id) { this.id = id; }
    public void setUrl(String url) { this.url = url; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
