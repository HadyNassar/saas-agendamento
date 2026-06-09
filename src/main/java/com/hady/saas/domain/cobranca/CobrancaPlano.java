package com.hady.saas.domain.cobranca;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobrancas_plano")
public class CobrancaPlano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    // STANDARD | ELITE | PLATINUM
    @Column(nullable = false)
    private String plano;

    // RECORRENTE | AVULSO
    @Column(name = "tipo_cobranca", nullable = false)
    private String tipoCobranca;

    @Column(name = "mp_payment_id")
    private String mpPaymentId;

    @Column(name = "mp_subscription_id")
    private String mpSubscriptionId;

    @Column(nullable = false)
    private BigDecimal valor;

    // PENDENTE | AUTORIZADO | CANCELADO | REJEITADO
    @Column(nullable = false)
    private String status = "PENDENTE";

    @Column(name = "periodo_inicio")
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim")
    private LocalDate periodoFim;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public CobrancaPlano() {}

    // ── GETTERS ──────────────────────────────────────────────────────────────
    public Long getId()                         { return id; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }
    public String getPlano()                    { return plano; }
    public String getTipoCobranca()             { return tipoCobranca; }
    public String getMpPaymentId()              { return mpPaymentId; }
    public String getMpSubscriptionId()         { return mpSubscriptionId; }
    public BigDecimal getValor()                { return valor; }
    public String getStatus()                   { return status; }
    public LocalDate getPeriodoInicio()         { return periodoInicio; }
    public LocalDate getPeriodoFim()            { return periodoFim; }
    public LocalDateTime getCriadoEm()          { return criadoEm; }

    // ── SETTERS ──────────────────────────────────────────────────────────────
    public void setEstabelecimento(Estabelecimento e) { this.estabelecimento = e; }
    public void setPlano(String v)                    { this.plano = v; }
    public void setTipoCobranca(String v)             { this.tipoCobranca = v; }
    public void setMpPaymentId(String v)              { this.mpPaymentId = v; }
    public void setMpSubscriptionId(String v)         { this.mpSubscriptionId = v; }
    public void setValor(BigDecimal v)                { this.valor = v; }
    public void setStatus(String v)                   { this.status = v; }
    public void setPeriodoInicio(LocalDate v)         { this.periodoInicio = v; }
    public void setPeriodoFim(LocalDate v)            { this.periodoFim = v; }
}
