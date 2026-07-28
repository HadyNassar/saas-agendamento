package com.hady.saas.application.dto;

import com.hady.saas.domain.whatsapp.WhatsAppMensagem;

import java.time.LocalDateTime;

/**
 * Item 5 (camada ELITE): representação de uma mensagem individual dentro do
 * histórico de uma conversa, para o inbox do painel. Mesmo padrão de
 * ConversaWhatsAppResumoDTO — DTO simples, sem lógica.
 */
public class WhatsAppMensagemInboxDTO {

    private final Long id;
    private final String mensagem;
    private final String tipo;
    private final String status;
    private final String direcao; // ENVIADA ou RECEBIDA
    private final LocalDateTime enviadoEm;
    private final LocalDateTime criadoEm;

    public WhatsAppMensagemInboxDTO(WhatsAppMensagem mensagem) {
        this.id = mensagem.getId();
        this.mensagem = mensagem.getMensagem();
        this.tipo = mensagem.getTipo();
        this.status = mensagem.getStatus();
        this.direcao = mensagem.getDirecao();
        this.enviadoEm = mensagem.getEnviadoEm();
        this.criadoEm = mensagem.getCriadoEm();
    }

    public Long getId() { return id; }
    public String getMensagem() { return mensagem; }
    public String getTipo() { return tipo; }
    public String getStatus() { return status; }
    public String getDirecao() { return direcao; }
    public LocalDateTime getEnviadoEm() { return enviadoEm; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}