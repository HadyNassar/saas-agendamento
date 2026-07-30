package com.hady.saas.application.dto;

import com.hady.saas.domain.enums.EstagioCliente;
import com.hady.saas.domain.whatsapp.CampanhaEnvio;

import java.time.LocalDateTime;

/**
 * Resumo de uma CampanhaEnvio para exibição no painel (criar, listar,
 * buscar, confirmar, cancelar — CampanhaEnvioController).
 *
 * Espelha os campos de leitura de CampanhaEnvio; não expõe a entidade JPA
 * diretamente para evitar acoplar a API ao modelo de persistência.
 */
public class CampanhaEnvioResumoDTO {

    private Long id;
    private EstagioCliente filtroEstagio;
    private String mensagem;
    private boolean usouTemplate;
    private String origem;
    private String status;
    private int totalDestinatarios;
    private int totalEnviados;
    private int totalErros;
    private LocalDateTime criadoEm;
    private LocalDateTime concluidoEm;

    public CampanhaEnvioResumoDTO() {}

    public CampanhaEnvioResumoDTO(CampanhaEnvio campanha) {
        this.id = campanha.getId();
        this.filtroEstagio = campanha.getFiltroEstagio();
        this.mensagem = campanha.getMensagem();
        this.usouTemplate = campanha.isUsouTemplate();
        this.origem = campanha.getOrigem();
        this.status = campanha.getStatus();
        this.totalDestinatarios = campanha.getTotalDestinatarios();
        this.totalEnviados = campanha.getTotalEnviados();
        this.totalErros = campanha.getTotalErros();
        this.criadoEm = campanha.getCriadoEm();
        this.concluidoEm = campanha.getConcluidoEm();
    }

    public Long getId()                         { return id; }
    public EstagioCliente getFiltroEstagio()     { return filtroEstagio; }
    public String getMensagem()                  { return mensagem; }
    public boolean isUsouTemplate()               { return usouTemplate; }
    public String getOrigem()                    { return origem; }
    public String getStatus()                    { return status; }
    public int getTotalDestinatarios()           { return totalDestinatarios; }
    public int getTotalEnviados()                { return totalEnviados; }
    public int getTotalErros()                   { return totalErros; }
    public LocalDateTime getCriadoEm()           { return criadoEm; }
    public LocalDateTime getConcluidoEm()        { return concluidoEm; }

    public void setId(Long id)                                     { this.id = id; }
    public void setFiltroEstagio(EstagioCliente filtroEstagio)     { this.filtroEstagio = filtroEstagio; }
    public void setMensagem(String mensagem)                       { this.mensagem = mensagem; }
    public void setUsouTemplate(boolean usouTemplate)               { this.usouTemplate = usouTemplate; }
    public void setOrigem(String origem)                           { this.origem = origem; }
    public void setStatus(String status)                           { this.status = status; }
    public void setTotalDestinatarios(int totalDestinatarios)      { this.totalDestinatarios = totalDestinatarios; }
    public void setTotalEnviados(int totalEnviados)                { this.totalEnviados = totalEnviados; }
    public void setTotalErros(int totalErros)                      { this.totalErros = totalErros; }
    public void setCriadoEm(LocalDateTime criadoEm)                { this.criadoEm = criadoEm; }
    public void setConcluidoEm(LocalDateTime concluidoEm)          { this.concluidoEm = concluidoEm; }
}