package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotBlank;

// Recebido quando um cliente final tenta agendar em um salão AINDA NÃO
// cadastrado no sistema (pin cinza). Usado depois como argumento comercial
// na abordagem ao dono do salão.
public class RegistrarInteresseDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String nomeCliente;

    @NotBlank(message = "Contato é obrigatório")
    private String contato; // WhatsApp ou email do cliente final

    @NotBlank(message = "Identificação do salão é obrigatória")
    private String googlePlaceId;

    @NotBlank(message = "Nome do salão é obrigatório")
    private String nomeSalaoExterno;

    public RegistrarInteresseDTO() {}

    public String getNomeCliente()                        { return nomeCliente; }
    public void setNomeCliente(String nomeCliente)        { this.nomeCliente = nomeCliente; }

    public String getContato()                             { return contato; }
    public void setContato(String contato)                 { this.contato = contato; }

    public String getGooglePlaceId()                       { return googlePlaceId; }
    public void setGooglePlaceId(String googlePlaceId)     { this.googlePlaceId = googlePlaceId; }

    public String getNomeSalaoExterno()                          { return nomeSalaoExterno; }
    public void setNomeSalaoExterno(String nomeSalaoExterno)    { this.nomeSalaoExterno = nomeSalaoExterno; }
}