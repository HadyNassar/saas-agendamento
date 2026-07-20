package com.hady.saas.application.dto;

import java.time.LocalDateTime;

public class LogAuditoriaDTO {

    private Long id;
    private String adminEmail;
    private String acao;
    private Long estabelecimentoId;
    private String nomeEstabelecimento; // preenchido via JOIN, pode vir null se estabelecimentoId for null
    private String detalhes;
    private LocalDateTime criadoEm;

    public LogAuditoriaDTO() {}

    public LogAuditoriaDTO(Long id, String adminEmail, String acao, Long estabelecimentoId,
                           String nomeEstabelecimento, String detalhes, LocalDateTime criadoEm) {
        this.id = id;
        this.adminEmail = adminEmail;
        this.acao = acao;
        this.estabelecimentoId = estabelecimentoId;
        this.nomeEstabelecimento = nomeEstabelecimento;
        this.detalhes = detalhes;
        this.criadoEm = criadoEm;
    }

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public String getAdminEmail()                     { return adminEmail; }
    public void setAdminEmail(String adminEmail)       { this.adminEmail = adminEmail; }

    public String getAcao()                            { return acao; }
    public void setAcao(String acao)                   { this.acao = acao; }

    public Long getEstabelecimentoId()                 { return estabelecimentoId; }
    public void setEstabelecimentoId(Long v)           { this.estabelecimentoId = v; }

    public String getNomeEstabelecimento()                  { return nomeEstabelecimento; }
    public void setNomeEstabelecimento(String v)            { this.nomeEstabelecimento = v; }

    public String getDetalhes()                        { return detalhes; }
    public void setDetalhes(String detalhes)           { this.detalhes = detalhes; }

    public LocalDateTime getCriadoEm()                  { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm)     { this.criadoEm = criadoEm; }
}