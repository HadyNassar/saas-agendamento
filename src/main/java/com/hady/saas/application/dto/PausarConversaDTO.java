package com.hady.saas.application.dto;

public class PausarConversaDTO {

    // Minutos até a pausa expirar automaticamente. Null = pausa sem prazo
    // (só volta quando o dono clicar em "retomar" manualmente).
    private Integer minutos;

    public Integer getMinutos() { return minutos; }
    public void setMinutos(Integer minutos) { this.minutos = minutos; }
}