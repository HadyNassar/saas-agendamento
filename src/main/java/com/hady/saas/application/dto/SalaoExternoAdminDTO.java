package com.hady.saas.application.dto;

// Versão ADMIN do salão externo — inclui avaliação e (quando disponível)
// telefone. Usado exclusivamente nos endpoints protegidos por login
// (autenticados como ADMIN), nunca nos endpoints públicos do mapa.
public class SalaoExternoAdminDTO {

    private String googlePlaceId;
    private String nome;
    private String endereco;
    private double latitude;
    private double longitude;
    private Double avaliacaoGoogle;
    private String telefone; // populado sob demanda via /telefone, pode vir null aqui

    public SalaoExternoAdminDTO() {}

    public SalaoExternoAdminDTO(String googlePlaceId, String nome, String endereco,
                                double latitude, double longitude,
                                Double avaliacaoGoogle, String telefone) {
        this.googlePlaceId = googlePlaceId;
        this.nome = nome;
        this.endereco = endereco;
        this.latitude = latitude;
        this.longitude = longitude;
        this.avaliacaoGoogle = avaliacaoGoogle;
        this.telefone = telefone;
    }

    public String getGooglePlaceId()                    { return googlePlaceId; }
    public void setGooglePlaceId(String googlePlaceId)  { this.googlePlaceId = googlePlaceId; }

    public String getNome()                             { return nome; }
    public void setNome(String nome)                    { this.nome = nome; }

    public String getEndereco()                         { return endereco; }
    public void setEndereco(String endereco)            { this.endereco = endereco; }

    public double getLatitude()                          { return latitude; }
    public void setLatitude(double latitude)             { this.latitude = latitude; }

    public double getLongitude()                          { return longitude; }
    public void setLongitude(double longitude)             { this.longitude = longitude; }

    public Double getAvaliacaoGoogle()                        { return avaliacaoGoogle; }
    public void setAvaliacaoGoogle(Double avaliacaoGoogle)    { this.avaliacaoGoogle = avaliacaoGoogle; }

    public String getTelefone()                          { return telefone; }
    public void setTelefone(String telefone)             { this.telefone = telefone; }
}