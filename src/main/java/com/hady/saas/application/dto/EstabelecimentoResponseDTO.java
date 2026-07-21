package com.hady.saas.application.dto;

import java.math.BigDecimal;
import java.util.List;

public class EstabelecimentoResponseDTO {

    private Long id;
    private String nome;
    private String slug;
    private String telefone;
    private String endereco;

    // ── Endereço estruturado — precisa vir aqui pra tela de Configurações
    // conseguir pré-preencher CEP/logradouro/número/bairro/cidade/UF ao
    // carregar (antes só existia o campo de texto livre 'endereco'). ──────
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;

    private Integer mesesAntecipacao;
    private boolean whatsappConectado;
    private String plano;

    // ─── Campos de pagamento e sinal ─────────────────────────────────────────
    private boolean mpConectado;
    private boolean aceitaPagamento;
    private boolean exigirSinal;
    private String tipoSinal;
    private BigDecimal valorSinal;

    // ─── Campos de Detalhes & Redes ──
    private String fotoPerfil;
    private String instagram;
    private String whatsappContato;
    private String facebook;
    private List<String> comodidades;
    private List<String> formasPagamento;
    private String tipo;

    public EstabelecimentoResponseDTO() {}

    public EstabelecimentoResponseDTO(Long id, String nome, String slug,
                                      String telefone, String endereco,
                                      String cep, String logradouro, String numero,
                                      String complemento, String bairro, String cidade, String estado,
                                      Integer mesesAntecipacao, boolean whatsappConectado,
                                      String plano,
                                      boolean mpConectado, boolean aceitaPagamento,
                                      boolean exigirSinal, String tipoSinal, BigDecimal valorSinal,
                                      String fotoPerfil, String instagram, String whatsappContato,
                                      String facebook, List<String> comodidades, List<String> formasPagamento,
                                      String tipo) {
        this.id = id;
        this.nome = nome;
        this.slug = slug;
        this.telefone = telefone;
        this.endereco = endereco;
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.mesesAntecipacao = mesesAntecipacao;
        this.whatsappConectado = whatsappConectado;
        this.plano = plano;
        this.mpConectado = mpConectado;
        this.aceitaPagamento = aceitaPagamento;
        this.exigirSinal = exigirSinal;
        this.tipoSinal = tipoSinal;
        this.valorSinal = valorSinal;
        this.fotoPerfil = fotoPerfil;
        this.instagram = instagram;
        this.whatsappContato = whatsappContato;
        this.facebook = facebook;
        this.comodidades = comodidades;
        this.formasPagamento = formasPagamento;
        this.tipo = tipo;
    }

    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }

    public String getNome()                { return nome; }
    public void setNome(String nome)       { this.nome = nome; }

    public String getSlug()                { return slug; }
    public void setSlug(String slug)       { this.slug = slug; }

    public String getTelefone()            { return telefone; }
    public void setTelefone(String t)      { this.telefone = t; }

    public String getEndereco()            { return endereco; }
    public void setEndereco(String e)      { this.endereco = e; }

    public String getCep()                 { return cep; }
    public void setCep(String cep)         { this.cep = cep; }

    public String getLogradouro()                  { return logradouro; }
    public void setLogradouro(String logradouro)   { this.logradouro = logradouro; }

    public String getNumero()                      { return numero; }
    public void setNumero(String numero)           { this.numero = numero; }

    public String getComplemento()                 { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getBairro()                      { return bairro; }
    public void setBairro(String bairro)           { this.bairro = bairro; }

    public String getCidade()                      { return cidade; }
    public void setCidade(String cidade)           { this.cidade = cidade; }

    public String getEstado()                      { return estado; }
    public void setEstado(String estado)           { this.estado = estado; }

    public Integer getMesesAntecipacao()           { return mesesAntecipacao; }
    public void setMesesAntecipacao(Integer m)     { this.mesesAntecipacao = m; }

    public boolean isWhatsappConectado()           { return whatsappConectado; }
    public void setWhatsappConectado(boolean w)    { this.whatsappConectado = w; }

    public String getPlano()               { return plano; }
    public void setPlano(String plano)     { this.plano = plano; }

    public boolean isMpConectado()                 { return mpConectado; }
    public void setMpConectado(boolean mpConectado){ this.mpConectado = mpConectado; }

    public boolean isAceitaPagamento()             { return aceitaPagamento; }
    public void setAceitaPagamento(boolean a)      { this.aceitaPagamento = a; }

    public boolean isExigirSinal()                 { return exigirSinal; }
    public void setExigirSinal(boolean e)          { this.exigirSinal = e; }

    public String getTipoSinal()                   { return tipoSinal; }
    public void setTipoSinal(String tipoSinal)     { this.tipoSinal = tipoSinal; }

    public BigDecimal getValorSinal()                  { return valorSinal; }
    public void setValorSinal(BigDecimal valorSinal)   { this.valorSinal = valorSinal; }

    public String getFotoPerfil()                      { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil)        { this.fotoPerfil = fotoPerfil; }

    public String getInstagram()                        { return instagram; }
    public void setInstagram(String instagram)          { this.instagram = instagram; }

    public String getWhatsappContato()                          { return whatsappContato; }
    public void setWhatsappContato(String whatsappContato)      { this.whatsappContato = whatsappContato; }

    public String getFacebook()                        { return facebook; }
    public void setFacebook(String facebook)            { this.facebook = facebook; }

    public List<String> getComodidades()                        { return comodidades; }
    public void setComodidades(List<String> comodidades)        { this.comodidades = comodidades; }

    public List<String> getFormasPagamento()                            { return formasPagamento; }
    public void setFormasPagamento(List<String> formasPagamento)        { this.formasPagamento = formasPagamento; }

    public String getTipo()                { return tipo; }
    public void setTipo(String tipo)       { this.tipo = tipo; }
}