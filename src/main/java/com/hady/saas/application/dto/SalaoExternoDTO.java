package com.hady.saas.application.dto;

// Representa um salão encontrado via Google Places que NÃO está
// cadastrado no sistema — usado para os pins cinza do mapa.
public class SalaoExternoDTO {

    private String googlePlaceId;
    private String nome;
    private String endereco;
    private double latitude;
    private double longitude;
    private Double avaliacaoGoogle; // pode ser null se o local não tiver avaliações
    private String telefone;        // pode ser null — Places nem sempre retorna

    public SalaoExternoDTO() {}

    public SalaoExternoDTO(String googlePlaceId, String nome, String endereco,
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