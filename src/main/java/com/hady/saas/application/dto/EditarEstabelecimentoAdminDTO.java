package com.hady.saas.application.dto;

import java.math.BigDecimal;
import java.util.List;

// Recebido no PUT de edição administrativa — todos os campos são opcionais
// (nulls são ignorados), então o admin master pode atualizar só o que
// precisar sem sobrescrever o resto.
public class EditarEstabelecimentoAdminDTO {

    private String nome;
    private String tipo;
    private String telefone;
    private String whatsappContato;
    private String instagram;
    private String facebook;
    private String fotoPerfil;

    // Endereço estruturado — se cep vier preenchido, regeocodifica
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;

    private Integer mesesAntecipacao;
    private List<String> comodidades;
    private List<String> formasPagamento;

    private Boolean ativo;
    private Boolean exigirSinal;
    private String tipoSinal;
    private BigDecimal valorSinal;

    public EditarEstabelecimentoAdminDTO() {}

    public String getNome()                          { return nome; }
    public void setNome(String v)                    { this.nome = v; }

    public String getTipo()                          { return tipo; }
    public void setTipo(String v)                    { this.tipo = v; }

    public String getTelefone()                      { return telefone; }
    public void setTelefone(String v)                { this.telefone = v; }

    public String getWhatsappContato()                { return whatsappContato; }
    public void setWhatsappContato(String v)          { this.whatsappContato = v; }

    public String getInstagram()                     { return instagram; }
    public void setInstagram(String v)                { this.instagram = v; }

    public String getFacebook()                      { return facebook; }
    public void setFacebook(String v)                { this.facebook = v; }

    public String getFotoPerfil()                    { return fotoPerfil; }
    public void setFotoPerfil(String v)              { this.fotoPerfil = v; }

    public String getCep()                            { return cep; }
    public void setCep(String v)                      { this.cep = v; }

    public String getLogradouro()                     { return logradouro; }
    public void setLogradouro(String v)               { this.logradouro = v; }

    public String getNumero()                         { return numero; }
    public void setNumero(String v)                   { this.numero = v; }

    public String getComplemento()                    { return complemento; }
    public void setComplemento(String v)              { this.complemento = v; }

    public String getBairro()                         { return bairro; }
    public void setBairro(String v)                   { this.bairro = v; }

    public String getCidade()                         { return cidade; }
    public void setCidade(String v)                   { this.cidade = v; }

    public String getEstado()                         { return estado; }
    public void setEstado(String v)                   { this.estado = v; }

    public Integer getMesesAntecipacao()              { return mesesAntecipacao; }
    public void setMesesAntecipacao(Integer v)        { this.mesesAntecipacao = v; }

    public List<String> getComodidades()              { return comodidades; }
    public void setComodidades(List<String> v)        { this.comodidades = v; }

    public List<String> getFormasPagamento()          { return formasPagamento; }
    public void setFormasPagamento(List<String> v)    { this.formasPagamento = v; }

    public Boolean getAtivo()                          { return ativo; }
    public void setAtivo(Boolean v)                    { this.ativo = v; }

    public Boolean getExigirSinal()                    { return exigirSinal; }
    public void setExigirSinal(Boolean v)              { this.exigirSinal = v; }

    public String getTipoSinal()                       { return tipoSinal; }
    public void setTipoSinal(String v)                 { this.tipoSinal = v; }

    public BigDecimal getValorSinal()                   { return valorSinal; }
    public void setValorSinal(BigDecimal v)             { this.valorSinal = v; }
}