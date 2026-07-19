package com.hady.saas.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Visão COMPLETA de um estabelecimento para o admin master — inclui tudo
// que o próprio dono do salão vê, mais campos administrativos internos
// (trial, status de plano, MP, contagens). Usado na tela de detalhe/edição.
public class EstabelecimentoAdminDTO {

    // ── Identificação ────────────────────────────────────────────────
    private Long id;
    private String nome;
    private String slug;
    private String tipo;
    private LocalDateTime criadoEm;
    private boolean ativo;

    // ── Contato ───────────────────────────────────────────────────────
    private String telefone;
    private String whatsappContato;
    private boolean whatsappConectado;
    private String instagram;
    private String facebook;
    private String fotoPerfil;

    // ── Endereço estruturado ────────────────────────────────────────────
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String enderecoCompleto; // fallback de texto livre
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;

    // ── Plano e assinatura ───────────────────────────────────────────
    private String plano;
    private boolean planoAtivo;
    private LocalDate planoValidoAte;
    private String mpSubscriptionId;
    private boolean mpConectado;
    private boolean aceitaPagamento;
    private boolean exigirSinal;
    private String tipoSinal;
    private BigDecimal valorSinal;

    // ── Trial ─────────────────────────────────────────────────────────
    private boolean trialAtivo;
    private LocalDate trialInicio;
    private LocalDate trialFim;
    private boolean trialUsado;

    // ── Configurações ────────────────────────────────────────────────
    private Integer mesesAntecipacao;
    private List<String> comodidades;
    private List<String> formasPagamento;

    // ── Contadores (para dar contexto rápido sem abrir cada aba) ──────
    private long totalUsuarios;
    private long totalClientes;
    private long totalAgendamentos;
    private long visualizacoesMapaTotal;

    public EstabelecimentoAdminDTO() {}

    // GETTERS
    public Long getId()                              { return id; }
    public String getNome()                          { return nome; }
    public String getSlug()                          { return slug; }
    public String getTipo()                          { return tipo; }
    public LocalDateTime getCriadoEm()               { return criadoEm; }
    public boolean isAtivo()                         { return ativo; }
    public String getTelefone()                      { return telefone; }
    public String getWhatsappContato()               { return whatsappContato; }
    public boolean isWhatsappConectado()             { return whatsappConectado; }
    public String getInstagram()                     { return instagram; }
    public String getFacebook()                      { return facebook; }
    public String getFotoPerfil()                    { return fotoPerfil; }
    public String getCep()                           { return cep; }
    public String getLogradouro()                    { return logradouro; }
    public String getNumero()                        { return numero; }
    public String getComplemento()                   { return complemento; }
    public String getBairro()                        { return bairro; }
    public String getCidade()                        { return cidade; }
    public String getEstado()                        { return estado; }
    public String getEnderecoCompleto()              { return enderecoCompleto; }
    public BigDecimal getLatitude()                  { return latitude; }
    public BigDecimal getLongitude()                 { return longitude; }
    public String getPlano()                         { return plano; }
    public boolean isPlanoAtivo()                    { return planoAtivo; }
    public LocalDate getPlanoValidoAte()             { return planoValidoAte; }
    public String getMpSubscriptionId()              { return mpSubscriptionId; }
    public boolean isMpConectado()                   { return mpConectado; }
    public boolean isAceitaPagamento()               { return aceitaPagamento; }
    public boolean isExigirSinal()                   { return exigirSinal; }
    public String getTipoSinal()                     { return tipoSinal; }
    public BigDecimal getValorSinal()                { return valorSinal; }
    public boolean isTrialAtivo()                    { return trialAtivo; }
    public LocalDate getTrialInicio()                { return trialInicio; }
    public LocalDate getTrialFim()                   { return trialFim; }
    public boolean isTrialUsado()                    { return trialUsado; }
    public Integer getMesesAntecipacao()             { return mesesAntecipacao; }
    public List<String> getComodidades()             { return comodidades; }
    public List<String> getFormasPagamento()         { return formasPagamento; }
    public long getTotalUsuarios()                   { return totalUsuarios; }
    public long getTotalClientes()                   { return totalClientes; }
    public long getTotalAgendamentos()               { return totalAgendamentos; }
    public long getVisualizacoesMapaTotal()          { return visualizacoesMapaTotal; }

    // SETTERS
    public void setId(Long v)                             { this.id = v; }
    public void setNome(String v)                         { this.nome = v; }
    public void setSlug(String v)                         { this.slug = v; }
    public void setTipo(String v)                         { this.tipo = v; }
    public void setCriadoEm(LocalDateTime v)              { this.criadoEm = v; }
    public void setAtivo(boolean v)                       { this.ativo = v; }
    public void setTelefone(String v)                     { this.telefone = v; }
    public void setWhatsappContato(String v)              { this.whatsappContato = v; }
    public void setWhatsappConectado(boolean v)           { this.whatsappConectado = v; }
    public void setInstagram(String v)                    { this.instagram = v; }
    public void setFacebook(String v)                     { this.facebook = v; }
    public void setFotoPerfil(String v)                   { this.fotoPerfil = v; }
    public void setCep(String v)                          { this.cep = v; }
    public void setLogradouro(String v)                   { this.logradouro = v; }
    public void setNumero(String v)                       { this.numero = v; }
    public void setComplemento(String v)                  { this.complemento = v; }
    public void setBairro(String v)                       { this.bairro = v; }
    public void setCidade(String v)                       { this.cidade = v; }
    public void setEstado(String v)                       { this.estado = v; }
    public void setEnderecoCompleto(String v)             { this.enderecoCompleto = v; }
    public void setLatitude(BigDecimal v)                 { this.latitude = v; }
    public void setLongitude(BigDecimal v)                { this.longitude = v; }
    public void setPlano(String v)                        { this.plano = v; }
    public void setPlanoAtivo(boolean v)                  { this.planoAtivo = v; }
    public void setPlanoValidoAte(LocalDate v)            { this.planoValidoAte = v; }
    public void setMpSubscriptionId(String v)             { this.mpSubscriptionId = v; }
    public void setMpConectado(boolean v)                 { this.mpConectado = v; }
    public void setAceitaPagamento(boolean v)             { this.aceitaPagamento = v; }
    public void setExigirSinal(boolean v)                 { this.exigirSinal = v; }
    public void setTipoSinal(String v)                    { this.tipoSinal = v; }
    public void setValorSinal(BigDecimal v)               { this.valorSinal = v; }
    public void setTrialAtivo(boolean v)                  { this.trialAtivo = v; }
    public void setTrialInicio(LocalDate v)               { this.trialInicio = v; }
    public void setTrialFim(LocalDate v)                  { this.trialFim = v; }
    public void setTrialUsado(boolean v)                  { this.trialUsado = v; }
    public void setMesesAntecipacao(Integer v)            { this.mesesAntecipacao = v; }
    public void setComodidades(List<String> v)            { this.comodidades = v; }
    public void setFormasPagamento(List<String> v)        { this.formasPagamento = v; }
    public void setTotalUsuarios(long v)                  { this.totalUsuarios = v; }
    public void setTotalClientes(long v)                  { this.totalClientes = v; }
    public void setTotalAgendamentos(long v)              { this.totalAgendamentos = v; }
    public void setVisualizacoesMapaTotal(long v)         { this.visualizacoesMapaTotal = v; }
}