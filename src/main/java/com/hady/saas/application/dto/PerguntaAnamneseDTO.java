package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

// DTO para criar/editar pergunta
public class PerguntaAnamneseDTO {

    @NotBlank(message = "Texto da pergunta obrigatório")
    private String texto;

    @NotBlank(message = "Tipo obrigatório")
    private String tipo; // SIM_NAO, SIM_NAO_DETALHE, TEXTO_LIVRE

    private int ordem = 0;

    // GETTERS
    public String getTexto() { return texto; }
    public String getTipo() { return tipo; }
    public int getOrdem() { return ordem; }

    // SETTERS
    public void setTexto(String texto) { this.texto = texto; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
}
