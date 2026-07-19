package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Passo 2 do login do admin master: confirma o código de 6 dígitos
// recebido por email. Só então o JWT completo é emitido.
public class ConfirmarCodigo2FaDTO {

    @NotNull(message = "Usuário obrigatório")
    private Long usuarioId;

    @NotBlank(message = "Código obrigatório")
    private String codigo;

    public Long getUsuarioId()                { return usuarioId; }
    public void setUsuarioId(Long usuarioId)  { this.usuarioId = usuarioId; }

    public String getCodigo()                 { return codigo; }
    public void setCodigo(String codigo)      { this.codigo = codigo; }
}