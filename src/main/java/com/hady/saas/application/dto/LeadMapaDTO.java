package com.hady.saas.application.dto;

import java.time.LocalDateTime;

// Um lead capturado no mapa — cliente final que tentou agendar em salão
// ainda não cadastrado. É a munição comercial pra abordar o dono do salão:
// "N pessoas tentaram te agendar pelo Agendara este mês".
public class LeadMapaDTO {

    private Long id;
    private String nomeCliente;
    private String contato;
    private String googlePlaceId;
    private String nomeSalaoExterno;
    private LocalDateTime dataHora;

    public LeadMapaDTO() {}

    public LeadMapaDTO(Long id, String nomeCliente, String contato, String googlePlaceId,
                       String nomeSalaoExterno, LocalDateTime dataHora) {
        this.id = id;
        this.nomeCliente = nomeCliente;
        this.contato = contato;
        this.googlePlaceId = googlePlaceId;
        this.nomeSalaoExterno = nomeSalaoExterno;
        this.dataHora = dataHora;
    }

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public String getNomeCliente()                    { return nomeCliente; }
    public void setNomeCliente(String nomeCliente)    { this.nomeCliente = nomeCliente; }

    public String getContato()                        { return contato; }
    public void setContato(String contato)            { this.contato = contato; }

    public String getGooglePlaceId()                        { return googlePlaceId; }
    public void setGooglePlaceId(String googlePlaceId)      { this.googlePlaceId = googlePlaceId; }

    public String getNomeSalaoExterno()                          { return nomeSalaoExterno; }
    public void setNomeSalaoExterno(String nomeSalaoExterno)    { this.nomeSalaoExterno = nomeSalaoExterno; }

    public LocalDateTime getDataHora()                { return dataHora; }
    public void setDataHora(LocalDateTime dataHora)   { this.dataHora = dataHora; }
}