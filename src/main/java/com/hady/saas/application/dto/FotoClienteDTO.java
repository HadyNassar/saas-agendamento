package com.hady.saas.application.dto;

import com.hady.saas.domain.fotocliente.FotoCliente;

import java.time.LocalDateTime;

public class FotoClienteDTO {

    private Long id;
    private String tipo;   // "ANTES" ou "DEPOIS"
    private String url;    // URL pública para acessar a imagem
    private LocalDateTime criadoEm;

    public static FotoClienteDTO from(FotoCliente foto, String baseUrl) {
        FotoClienteDTO dto = new FotoClienteDTO();
        dto.setId(foto.getId());
        dto.setTipo(foto.getTipo().name());
        // URL aponta para o endpoint autenticado — não mais para /uploads/** público.
        // O frontend deve enviar o JWT no header Authorization ao carregar esta imagem.
        dto.setUrl(baseUrl + "/painel/fotos/clientes/" + foto.getId() + "/serve");
        dto.setCriadoEm(foto.getCriadoEm());
        return dto;
    }


    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getUrl() { return url; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public void setId(Long id) { this.id = id; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setUrl(String url) { this.url = url; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
