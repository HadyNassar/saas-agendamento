package com.hady.saas.application.dto;

import java.math.BigDecimal;
import java.util.List;

// Perfil público completo de UM estabelecimento — usado na página
// encontre.oagendara.com.br/salao/{slug}
// Estabelecimento CADASTRADO: cliente vê tudo, incluindo telefone/WhatsApp.
// (A restrição de contato é só pra salão NÃO cadastrado — pin cinza —
// que usa o fluxo separado de "interesse" via ModalInteresse.)
public class EstabelecimentoPerfilPublicoDTO {

    private Long id;
    private String nome;
    private String slug;
    private String tipo;
    private String telefone;
    private String whatsappContato;
    private String instagram;
    private String facebook;
    private String fotoPerfil;

    private String logradouro;
    private String numero;
    private String bairro;
    private String cidade;
    private String estado;
    private BigDecimal latitude;
    private BigDecimal longitude;

    private List<String> comodidades;
    private List<String> formasPagamento;

    private boolean aceitaAgendamentoOnline;

    public EstabelecimentoPerfilPublicoDTO() {}

    public EstabelecimentoPerfilPublicoDTO(Long id, String nome, String slug, String tipo,
                                           String telefone, String whatsappContato,
                                           String instagram, String facebook, String fotoPerfil,
                                           String logradouro, String numero, String bairro,
                                           String cidade, String estado,
                                           BigDecimal latitude, BigDecimal longitude,
                                           List<String> comodidades, List<String> formasPagamento,
                                           boolean aceitaAgendamentoOnline) {
        this.id = id;
        this.nome = nome;
        this.slug = slug;
        this.tipo = tipo;
        this.telefone = telefone;
        this.whatsappContato = whatsappContato;
        this.instagram = instagram;
        this.facebook = facebook;
        this.fotoPerfil = fotoPerfil;
        this.logradouro = logradouro;
        this.numero = numero;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.latitude = latitude;
        this.longitude = longitude;
        this.comodidades = comodidades;
        this.formasPagamento = formasPagamento;
        this.aceitaAgendamentoOnline = aceitaAgendamentoOnline;
    }

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public String getNome()                      { return nome; }
    public void setNome(String nome)             { this.nome = nome; }

    public String getSlug()                      { return slug; }
    public void setSlug(String slug)             { this.slug = slug; }

    public String getTipo()                      { return tipo; }
    public void setTipo(String tipo)             { this.tipo = tipo; }

    public String getTelefone()                  { return telefone; }
    public void setTelefone(String telefone)     { this.telefone = telefone; }

    public String getWhatsappContato()                     { return whatsappContato; }
    public void setWhatsappContato(String whatsappContato) { this.whatsappContato = whatsappContato; }

    public String getInstagram()                 { return instagram; }
    public void setInstagram(String instagram)   { this.instagram = instagram; }

    public String getFacebook()                  { return facebook; }
    public void setFacebook(String facebook)     { this.facebook = facebook; }

    public String getFotoPerfil()                { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }

    public String getLogradouro()                { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getNumero()                    { return numero; }
    public void setNumero(String numero)         { this.numero = numero; }

    public String getBairro()                    { return bairro; }
    public void setBairro(String bairro)         { this.bairro = bairro; }

    public String getCidade()                    { return cidade; }
    public void setCidade(String cidade)         { this.cidade = cidade; }

    public String getEstado()                    { return estado; }
    public void setEstado(String estado)         { this.estado = estado; }

    public BigDecimal getLatitude()               { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude()               { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public List<String> getComodidades()                 { return comodidades; }
    public void setComodidades(List<String> comodidades) { this.comodidades = comodidades; }

    public List<String> getFormasPagamento()                     { return formasPagamento; }
    public void setFormasPagamento(List<String> formasPagamento) { this.formasPagamento = formasPagamento; }

    public boolean isAceitaAgendamentoOnline()          { return aceitaAgendamentoOnline; }
    public void setAceitaAgendamentoOnline(boolean a)   { this.aceitaAgendamentoOnline = a; }
}