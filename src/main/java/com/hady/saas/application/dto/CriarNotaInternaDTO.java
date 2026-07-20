package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotBlank;

public class CriarNotaInternaDTO {

    @NotBlank(message = "Texto da nota é obrigatório")
    private String texto;

    public String getTexto()             { return texto; }
    public void setTexto(String texto)   { this.texto = texto; }
}