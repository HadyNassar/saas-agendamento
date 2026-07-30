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

    // Mantido por compatibilidade (endereço completo em texto único).
    // Ainda é usado como fallback e para exibição rápida.
    private String endereco;

    // ── Endereço estruturado (preenchido via busca de CEP) ──────────────────
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;
    // ─────────────────────────────────────────────────────────────────────

    // ── Localização geográfica (populada automaticamente via Google Geocoding) ─
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    // Permite ocultar do mapa público sem desativar a conta inteira.
    // Controlado pelo admin master via Gestão do Mapa.
    @Column(name = "visivel_no_mapa")
    private boolean visivelNoMapa = true;
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

    // Toggle geral do robô conversacional (aba de controle — painel do dono).
    // NÃO afeta lembretes/confirmações automáticas (WhatsAppService), que são
    // um serviço à parte e continuam rodando sempre, mesmo com isso desligado.
    @Column(name = "robot_ativo", nullable = false)
    private boolean robotAtivo = true;

    // Item 8: cupom automático de reativação. Desligado por padrão — o dono
    // precisa ativar explicitamente e configurar o desconto (migration V26).
    // Quando ativo, CampanhaAutomaticaService gera um CupomEstabelecimento
    // novo pra cada cliente que virar INATIVO e anexa o link já com o cupom
    // aplicado na mensagem automática.
    @Column(name = "cupom_reativacao_automatico_ativo", nullable = false)
    private boolean cupomReativacaoAutomaticoAtivo = false;

    @Column(name = "cupom_reativacao_tipo_desconto", length = 20)
    private String cupomReativacaoTipoDesconto; // PERCENTUAL | VALOR_FIXO

    @Column(name = "cupom_reativacao_valor_desconto", precision = 10, scale = 2)
    private BigDecimal cupomReativacaoValorDesconto;

    // Item 17 (indicação/referral) — mesmo padrão do cupom de reativação
    // acima: opt-in do dono, tipo e valor de desconto configuráveis.
    // Quando ativo, WhatsAppService.enviarAgradecimentoNotaAlta (nota 5)
    // anexa um link de indicação único na mensagem. Quando o indicado
    // conclui o primeiro atendimento, DOIS cupons são gerados — um pro
    // indicador, um pro indicado — ambos com esse mesmo tipo/valor.
    @Column(name = "indicacao_automatica_ativa", nullable = false)
    private boolean indicacaoAutomaticaAtiva = false;

    @Column(name = "indicacao_tipo_desconto", length = 20)
    private String indicacaoTipoDesconto; // PERCENTUAL | VALOR_FIXO

    @Column(name = "indicacao_valor_desconto", precision = 10, scale = 2)
    private BigDecimal indicacaoValorDesconto;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "estabelecimento_comodidades", joinColumns = @JoinColumn(name = "estabelecimento_id"))
    @Column(name = "comodidade")
    private List<String> comodidades = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "estabelecimento_formas_pagamento", joinColumns = @JoinColumn(name = "estabelecimento_id"))
    @Column(name = "forma_pagamento")
    private List<String> formasPagamento = new ArrayList<>();

    // ── Campanhas de disparo em massa (seção 5-B) ───────────────────────────
    // Template universal opcional do estabelecimento, aplicado em toda
    // campanha manual. Suporta variáveis {{nome_cliente}}, {{nome_estabelecimento}}
    // e o placeholder obrigatório {{mensagem}}, onde entra o texto livre
    // digitado pelo dono na hora de criar a campanha. Se null/branco, o
    // sistema envia só o texto livre puro (comportamento antigo, sem template).
    @Column(name = "template_mensagem_campanha", columnDefinition = "TEXT")
    private String templateMensagemCampanha;

    // Intervalo mínimo (em dias) que o dono precisa esperar entre o disparo
    // de uma campanha manual e a próxima, para reduzir risco de banimento do
    // WhatsApp por volume/repetição. Configurável pelo dono, mínimo 1 dia.
    // Default 2 dias — ponto de partida conservador, não é limite oficial
    // documentado pela Evolution API/WhatsApp, só uma escolha de produto.
    @Column(name = "intervalo_minimo_campanha_dias", nullable = false)
    private int intervaloMinimoCampanhaDias = 2;

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

    // Monta o endereço completo em texto a partir dos campos estruturados.
    // Usado para geocodificação e para exibição quando 'endereco' livre não foi informado.
    public String getEnderecoCompleto() {
        if (logradouro == null || logradouro.isBlank()) {
            return endereco; // fallback pro campo antigo, se estruturado não existir
        }
        StringBuilder sb = new StringBuilder();
        sb.append(logradouro);
        if (numero != null && !numero.isBlank()) sb.append(", ").append(numero);
        if (bairro != null && !bairro.isBlank()) sb.append(" - ").append(bairro);
        if (cidade != null && !cidade.isBlank()) sb.append(", ").append(cidade);
        if (estado != null && !estado.isBlank()) sb.append(" - ").append(estado);
        if (cep != null && !cep.isBlank()) sb.append(", ").append(cep);
        return sb.toString();
    }

    // ── GETTERS ──────────────────────────────────────────────────────────────
    public Long getId()                        { return id; }
    public String getNome()                    { return nome; }
    public String getSlug()                    { return slug; }
    public TipoEstabelecimento getTipo()       { return tipo; }
    public Plano getPlano()                    { return plano; }
    public String getTelefone()                { return telefone; }
    public String getEndereco()                { return endereco; }
    public String getLogradouro()              { return logradouro; }
    public String getNumero()                  { return numero; }
    public String getComplemento()             { return complemento; }
    public String getBairro()                  { return bairro; }
    public String getCidade()                  { return cidade; }
    public String getEstado()                  { return estado; }
    public String getCep()                     { return cep; }
    public BigDecimal getLatitude()            { return latitude; }
    public BigDecimal getLongitude()           { return longitude; }
    public boolean isVisivelNoMapa()           { return visivelNoMapa; }
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
    public boolean isRobotAtivo()              { return robotAtivo; }
    public boolean isCupomReativacaoAutomaticoAtivo() { return cupomReativacaoAutomaticoAtivo; }
    public String getCupomReativacaoTipoDesconto()    { return cupomReativacaoTipoDesconto; }
    public BigDecimal getCupomReativacaoValorDesconto() { return cupomReativacaoValorDesconto; }
    public boolean isIndicacaoAutomaticaAtiva()       { return indicacaoAutomaticaAtiva; }
    public String getIndicacaoTipoDesconto()          { return indicacaoTipoDesconto; }
    public BigDecimal getIndicacaoValorDesconto()     { return indicacaoValorDesconto; }
    public List<String> getComodidades()       { return comodidades; }
    public List<String> getFormasPagamento()   { return formasPagamento; }
    public String getTemplateMensagemCampanha()     { return templateMensagemCampanha; }
    public int getIntervaloMinimoCampanhaDias()     { return intervaloMinimoCampanhaDias; }

    // ── SETTERS ──────────────────────────────────────────────────────────────
    public void setId(Long id)                                         { this.id = id; }
    public void setNome(String nome)                                   { this.nome = nome; }
    public void setSlug(String slug)                                   { this.slug = slug; }
    public void setTipo(TipoEstabelecimento tipo)                      { this.tipo = tipo; }
    public void setPlano(Plano plano)                                  { this.plano = plano; }
    public void setTelefone(String telefone)                           { this.telefone = telefone; }
    public void setEndereco(String endereco)                           { this.endereco = endereco; }
    public void setLogradouro(String logradouro)                       { this.logradouro = logradouro; }
    public void setNumero(String numero)                               { this.numero = numero; }
    public void setComplemento(String complemento)                     { this.complemento = complemento; }
    public void setBairro(String bairro)                               { this.bairro = bairro; }
    public void setCidade(String cidade)                               { this.cidade = cidade; }
    public void setEstado(String estado)                               { this.estado = estado; }
    public void setCep(String cep)                                     { this.cep = cep; }
    public void setLatitude(BigDecimal latitude)                       { this.latitude = latitude; }
    public void setLongitude(BigDecimal longitude)                     { this.longitude = longitude; }
    public void setVisivelNoMapa(boolean visivelNoMapa)               { this.visivelNoMapa = visivelNoMapa; }
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
    public void setRobotAtivo(boolean robotAtivo)                      { this.robotAtivo = robotAtivo; }
    public void setCupomReativacaoAutomaticoAtivo(boolean v)           { this.cupomReativacaoAutomaticoAtivo = v; }
    public void setCupomReativacaoTipoDesconto(String v)               { this.cupomReativacaoTipoDesconto = v; }
    public void setCupomReativacaoValorDesconto(BigDecimal v)          { this.cupomReativacaoValorDesconto = v; }
    public void setIndicacaoAutomaticaAtiva(boolean v)                 { this.indicacaoAutomaticaAtiva = v; }
    public void setIndicacaoTipoDesconto(String v)                     { this.indicacaoTipoDesconto = v; }
    public void setIndicacaoValorDesconto(BigDecimal v)                { this.indicacaoValorDesconto = v; }
    public void setComodidades(List<String> comodidades)               { this.comodidades = comodidades; }
    public void setFormasPagamento(List<String> formasPagamento)       { this.formasPagamento = formasPagamento; }
    public void setTemplateMensagemCampanha(String v)                  { this.templateMensagemCampanha = v; }
    public void setIntervaloMinimoCampanhaDias(int v)                  { this.intervaloMinimoCampanhaDias = v; }
}