package com.hady.saas.application.dto;

// Representa um salão encontrado via Google Places que NÃO está
// cadastrado no sistema — usado para os pins cinza do mapa PÚBLICO.
// Propositalmente NÃO expõe avaliação nem telefone ao cliente final —
// isso é reservado ao pin verde (cadastrado), como incentivo comercial
// para o dono do salão assinar o sistema.
public class SalaoExternoDTO {

    private String googlePlaceId;
    private String nome;
    private String endereco;
    private double latitude;
    private double longitude;

    public SalaoExternoDTO() {}

    public SalaoExternoDTO(String googlePlaceId, String nome, String endereco,
                           double latitude, double longitude) {
        this.googlePlaceId = googlePlaceId;
        this.nome = nome;
        this.endereco = endereco;
        this.latitude = latitude;
        this.longitude = longitude;
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
}