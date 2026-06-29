package com.hady.saas.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class EditarEstabelecimentoDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    private String telefone;
    private String endereco;

    @Min(value = 1, message = "Mínimo de 1 mês")
    @Max(value = 12, message = "Máximo de 12 meses")
    private Integer mesesAntecipacao;

    private String fotoPerfil;
    private String instagram;
    private String whatsappContato;
    private String facebook;
    private List<String> comodidades;
    private List<String> formasPagamento;

    public EditarEstabelecimentoDTO() {}

    public String getNome()                       { return nome; }
    public void setNome(String nome)              { this.nome = nome; }

    public String getTelefone()                   { return telefone; }
    public void setTelefone(String telefone)      { this.telefone = telefone; }

    public String getEndereco()                   { return endereco; }
    public void setEndereco(String endereco)      { this.endereco = endereco; }

    public Integer getMesesAntecipacao()          { return mesesAntecipacao; }
    public void setMesesAntecipacao(Integer m)    { this.mesesAntecipacao = m; }

    public String getFotoPerfil()                 { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil)  { this.fotoPerfil = fotoPerfil; }

    public String getInstagram()                  { return instagram; }
    public void setInstagram(String instagram)    { this.instagram = instagram; }

    public String getWhatsappContato()            { return whatsappContato; }
    public void setWhatsappContato(String w)      { this.whatsappContato = w; }

    public String getFacebook()                   { return facebook; }
    public void setFacebook(String facebook)      { this.facebook = facebook; }

    public List<String> getComodidades()          { return comodidades; }
    public void setComodidades(List<String> c)    { this.comodidades = c; }

    public List<String> getFormasPagamento()      { return formasPagamento; }
    public void setFormasPagamento(List<String> f){ this.formasPagamento = f; }
}