package com.hady.saas.application.dto;

/**
 * Item 5 (camada ELITE): corpo do POST /painel/whatsapp/robo/conversas/{id}/responder.
 * O dono do salão digita uma mensagem manual pra responder o cliente direto do painel.
 */
public class ResponderConversaDTO {

    private String mensagem;

    public ResponderConversaDTO() {}

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
}