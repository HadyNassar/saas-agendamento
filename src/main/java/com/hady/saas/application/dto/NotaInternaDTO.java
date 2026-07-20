package com.hady.saas.application.dto;

import java.time.LocalDateTime;

public class NotaInternaDTO {

    private Long id;
    private Long estabelecimentoId;
    private String texto;
    private String criadoPor;
    private LocalDateTime criadoEm;

    public NotaInternaDTO() {}

    public NotaInternaDTO(Long id, Long estabelecimentoId, String texto, String criadoPor, LocalDateTime criadoEm) {
        this.id = id;
        this.estabelecimentoId = estabelecimentoId;
        this.texto = texto;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
    }

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public Long getEstabelecimentoId()                { return estabelecimentoId; }
    public void setEstabelecimentoId(Long v)          { this.estabelecimentoId = v; }

    public String getTexto()                          { return texto; }
    public void setTexto(String texto)                { this.texto = texto; }

    public String getCriadoPor()                       { return criadoPor; }
    public void setCriadoPor(String criadoPor)         { this.criadoPor = criadoPor; }

    public LocalDateTime getCriadoEm()                  { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm)     { this.criadoEm = criadoEm; }
}