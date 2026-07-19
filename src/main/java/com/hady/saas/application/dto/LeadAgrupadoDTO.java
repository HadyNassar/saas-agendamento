package com.hady.saas.application.dto;

// Agrupamento de leads por salão externo — mostra quais salões não
// cadastrados têm mais demanda acumulada, priorizando quem abordar primeiro.
public class LeadAgrupadoDTO {

    private String googlePlaceId;
    private String nomeSalaoExterno;
    private long totalLeads;

    public LeadAgrupadoDTO() {}

    public LeadAgrupadoDTO(String googlePlaceId, String nomeSalaoExterno, long totalLeads) {
        this.googlePlaceId = googlePlaceId;
        this.nomeSalaoExterno = nomeSalaoExterno;
        this.totalLeads = totalLeads;
    }

    public String getGooglePlaceId()                        { return googlePlaceId; }
    public void setGooglePlaceId(String googlePlaceId)      { this.googlePlaceId = googlePlaceId; }

    public String getNomeSalaoExterno()                          { return nomeSalaoExterno; }
    public void setNomeSalaoExterno(String nomeSalaoExterno)    { this.nomeSalaoExterno = nomeSalaoExterno; }

    public long getTotalLeads()                       { return totalLeads; }
    public void setTotalLeads(long totalLeads)        { this.totalLeads = totalLeads; }
}