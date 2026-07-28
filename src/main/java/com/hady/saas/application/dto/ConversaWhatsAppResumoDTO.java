package com.hady.saas.application.dto;

import com.hady.saas.domain.whatsapp.ConversaWhatsApp;

import java.time.LocalDateTime;

public class ConversaWhatsAppResumoDTO {

    private final Long id;
    private final Long clienteId;
    private final String clienteNome;
    private final String telefone;
    private final String estado;
    private final String ultimaMensagemPreview;
    private final LocalDateTime ultimaMensagemEm;
    private final boolean pausado;
    private final LocalDateTime pausadoAte;

    public ConversaWhatsAppResumoDTO(ConversaWhatsApp conversa) {
        this.id = conversa.getId();
        this.clienteId = conversa.getCliente().getId();
        this.clienteNome = conversa.getCliente().getNome();
        this.telefone = conversa.getCliente().getTelefone();
        this.estado = conversa.getEstado();
        this.ultimaMensagemPreview = conversa.getUltimaMensagemPreview();
        this.ultimaMensagemEm = conversa.getUltimaMensagemEm();
        this.pausado = conversa.isRobotPausadoAgora();
        this.pausadoAte = conversa.getPausadoAte();
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public String getClienteNome() { return clienteNome; }
    public String getTelefone() { return telefone; }
    public String getEstado() { return estado; }
    public String getUltimaMensagemPreview() { return ultimaMensagemPreview; }
    public LocalDateTime getUltimaMensagemEm() { return ultimaMensagemEm; }
    public boolean isPausado() { return pausado; }
    public LocalDateTime getPausadoAte() { return pausadoAte; }
}