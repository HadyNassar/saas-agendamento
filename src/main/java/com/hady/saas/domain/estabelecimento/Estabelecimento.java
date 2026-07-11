package com.hady.saas.domain.estabelecimento;

import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.enums.TipoEstabelecimento;
import com.hady.saas.infrastructure.security.EncryptedStringConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estabelecimentos")
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, nullable = false)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEstabelecimento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plano plano;

    private String telefone;
    private String endereco;

    // ── Localização geográfica (populada automaticamente via Google Geocoding) ─
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    private String cidade;
    private String estado;
    private String cep;
    // ─────────────────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    private boolean ativo = true;

    @Column(unique = true)
    private String evolutionInstanceName;

    private boolean whatsappConectado = false;

    @Column(name = "meses_antecipacao", nullable = false)
    private Integer mesesAntecipacao = 3;

    @Column(name = "plano_ativo")
    private boolean planoAtivo = false;

    @Column(name = "plano_valido_ate")
    private LocalDate planoValidoAte;

    @Column(name = "mp_subscription_id")
    private String mpSubscriptionId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "mp_access_token")
    private String mpAccessToken;

    @Column(name = "mp_user_id")
    private String mpUserId;

    @Column(name = "mp_conectado")
    private boolean mpConectado = false;

    @Column(name = "aceita_pagamento")
    private boolean aceitaPagamento = false;

    @Column(name = "exigir_sinal")
    private boolean exigirSinal = false;

    @Column(name = "tipo_sinal")
    private String tipoSinal;

    @Column(name = "valor_sinal", precision = 10, scale = 2)
    private BigDecimal valorSinal;

    @Column(name = "trial_ativo")
    private boolean trialAtivo = false;

    @Column(name = "trial_inicio")
    private LocalDate trialInicio;

    @Column(name = "trial_fim")
    private LocalDate trialFim;

    @Column(name = "trial_usado")
    private boolean trialUsado = false;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(name = "instagram")
    private String instagram;

    @Column(name = "whatsapp_contato")
    private String whatsappContato;

    @Column(name = "facebook")
    private String facebook;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "estabelecimento_comodidades", joinColumns = @JoinColumn(name = "estabelecimento_id"))
    @Column(name = "comodidade")
    private List<String> comodidades = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "estabelecimento_formas_pagamento", joinColumns = @JoinColumn(name = "estabelecimento_id"))
    @Column(name = "forma_pagamento")
    private List<String> formasPagamento = new ArrayList<>();

    public Estabelecimento() {}

    public Plano getPlanoEfetivo() {
        if (trialAtivo && trialFim != null && !LocalDate.now().isAfter(trialFim)) {
            return Plano.ELITE;
        }
        return plano;
    }

    public boolean podeCadastrarUsuario(int totalUsuariosAtual) {
        return totalUsuariosAtual < getPlanoEfetivo().getLimiteUsuarios();
    }

    public boolean temAcesso() {
        if (planoAtivo && planoValidoAte != null && !LocalDate.now().isAfter(planoValidoAte)) return true;
        if (trialAtivo && trialFim != null && !LocalDate.now().isAfter(trialFim)) return true;
        return false;
    }

    public void iniciarTrial() {
        if (trialUsado) throw new RuntimeException("Trial já utilizado por este estabelecimento.");
        if (planoAtivo) throw new RuntimeException("Você já possui um plano ativo.");
        this.trialAtivo = true;
        this.trialUsado = true;
        this.trialInicio = LocalDate.now();
        this.trialFim = LocalDate.now().plusDays(5);
    }

    // ── GETTERS ──────────────────────────────────────────────────────────────
    public Long getId()                        { return id; }
    public String getNome()                    { return nome; }
    public String getSlug()                    { return slug; }
    public TipoEstabelecimento getTipo()       { return tipo; }
    public Plano getPlano()                    { return plano; }
    public String getTelefone()                { return telefone; }
    public String getEndereco()                { return endereco; }
    public BigDecimal getLatitude()            { return latitude; }
    public BigDecimal getLongitude()           { return longitude; }
    public String getCidade()                  { return cidade; }
    public String getEstado()                  { return estado; }
    public String getCep()                     { return cep; }
    public LocalDateTime getCriadoEm()         { return criadoEm; }
    public boolean isAtivo()                   { return ativo; }
    public String getEvolutionInstanceName()   { return evolutionInstanceName; }
    public boolean isWhatsappConectado()       { return whatsappConectado; }
    public Integer getMesesAntecipacao()       { return mesesAntecipacao; }
    public boolean isPlanoAtivo()              { return planoAtivo; }
    public LocalDate getPlanoValidoAte()       { return planoValidoAte; }
    public String getMpSubscriptionId()        { return mpSubscriptionId; }
    public String getMpAccessToken()           { return mpAccessToken; }
    public String getMpUserId()                { return mpUserId; }
    public boolean isMpConectado()             { return mpConectado; }
    public boolean isAceitaPagamento()         { return aceitaPagamento; }
    public boolean isExigirSinal()             { return exigirSinal; }
    public String getTipoSinal()               { return tipoSinal; }
    public BigDecimal getValorSinal()          { return valorSinal; }
    public boolean isTrialAtivo()              { return trialAtivo; }
    public LocalDate getTrialInicio()          { return trialInicio; }
    public LocalDate getTrialFim()             { return trialFim; }
    public boolean isTrialUsado()              { return trialUsado; }
    public String getFotoPerfil()              { return fotoPerfil; }
    public String getInstagram()               { return instagram; }
    public String getWhatsappContato()         { return whatsappContato; }
    public String getFacebook()                { return facebook; }
    public List<String> getComodidades()       { return comodidades; }
    public List<String> getFormasPagamento()   { return formasPagamento; }

    // ── SETTERS ──────────────────────────────────────────────────────────────
    public void setId(Long id)                                         { this.id = id; }
    public void setNome(String nome)                                   { this.nome = nome; }
    public void setSlug(String slug)                                   { this.slug = slug; }
    public void setTipo(TipoEstabelecimento tipo)                      { this.tipo = tipo; }
    public void setPlano(Plano plano)                                  { this.plano = plano; }
    public void setTelefone(String telefone)                           { this.telefone = telefone; }
    public void setEndereco(String endereco)                           { this.endereco = endereco; }
    public void setLatitude(BigDecimal latitude)                       { this.latitude = latitude; }
    public void setLongitude(BigDecimal longitude)                     { this.longitude = longitude; }
    public void setCidade(String cidade)                               { this.cidade = cidade; }
    public void setEstado(String estado)                               { this.estado = estado; }
    public void setCep(String cep)                                     { this.cep = cep; }
    public void setCriadoEm(LocalDateTime criadoEm)                   { this.criadoEm = criadoEm; }
    public void setAtivo(boolean ativo)                                { this.ativo = ativo; }
    public void setEvolutionInstanceName(String n)                     { this.evolutionInstanceName = n; }
    public void setWhatsappConectado(boolean whatsappConectado)        { this.whatsappConectado = whatsappConectado; }
    public void setMesesAntecipacao(Integer mesesAntecipacao)          { this.mesesAntecipacao = mesesAntecipacao; }
    public void setPlanoAtivo(boolean planoAtivo)                      { this.planoAtivo = planoAtivo; }
    public void setPlanoValidoAte(LocalDate planoValidoAte)            { this.planoValidoAte = planoValidoAte; }
    public void setMpSubscriptionId(String mpSubscriptionId)           { this.mpSubscriptionId = mpSubscriptionId; }
    public void setMpAccessToken(String mpAccessToken)                 { this.mpAccessToken = mpAccessToken; }
    public void setMpUserId(String mpUserId)                           { this.mpUserId = mpUserId; }
    public void setMpConectado(boolean mpConectado)                    { this.mpConectado = mpConectado; }
    public void setAceitaPagamento(boolean aceitaPagamento)            { this.aceitaPagamento = aceitaPagamento; }
    public void setExigirSinal(boolean exigirSinal)                    { this.exigirSinal = exigirSinal; }
    public void setTipoSinal(String tipoSinal)                         { this.tipoSinal = tipoSinal; }
    public void setValorSinal(BigDecimal valorSinal)                   { this.valorSinal = valorSinal; }
    public void setTrialAtivo(boolean trialAtivo)                      { this.trialAtivo = trialAtivo; }
    public void setTrialInicio(LocalDate trialInicio)                  { this.trialInicio = trialInicio; }
    public void setTrialFim(LocalDate trialFim)                        { this.trialFim = trialFim; }
    public void setTrialUsado(boolean trialUsado)                      { this.trialUsado = trialUsado; }
    public void setFotoPerfil(String fotoPerfil)                       { this.fotoPerfil = fotoPerfil; }
    public void setInstagram(String instagram)                         { this.instagram = instagram; }
    public void setWhatsappContato(String whatsappContato)             { this.whatsappContato = whatsappContato; }
    public void setFacebook(String facebook)                           { this.facebook = facebook; }
    public void setComodidades(List<String> comodidades)               { this.comodidades = comodidades; }
    public void setFormasPagamento(List<String> formasPagamento)       { this.formasPagamento = formasPagamento; }
}