package com.hady.saas.domain.transacao;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacoes")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @Column(name = "mp_payment_id")
    private String mpPaymentId;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    @Column(name = "taxa_plataforma", nullable = false)
    private BigDecimal taxaPlataforma;

    @Column(name = "valor_estabelecimento", nullable = false)
    private BigDecimal valorEstabelecimento;

    // PENDENTE | APROVADO | REJEITADO | REEMBOLSADO | CANCELADO
    @Column(nullable = false)
    private String status = "PENDENTE";

    // pix | credit_card | debit_card
    @Column(name = "metodo_pagamento")
    private String metodoPagamento;

    @Column(name = "pix_qr_code", columnDefinition = "TEXT")
    private String pixQrCode;

    @Column(name = "pix_qr_code_base64", columnDefinition = "TEXT")
    private String pixQrCodeBase64;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public Transacao() {}

    // ── GETTERS ──────────────────────────────────────────────────────────────
    public Long getId()                          { return id; }
    public Estabelecimento getEstabelecimento()  { return estabelecimento; }
    public Agendamento getAgendamento()          { return agendamento; }
    public String getMpPaymentId()               { return mpPaymentId; }
    public BigDecimal getValorTotal()            { return valorTotal; }
    public BigDecimal getTaxaPlataforma()        { return taxaPlataforma; }
    public BigDecimal getValorEstabelecimento()  { return valorEstabelecimento; }
    public String getStatus()                    { return status; }
    public String getMetodoPagamento()           { return metodoPagamento; }
    public String getPixQrCode()                 { return pixQrCode; }
    public String getPixQrCodeBase64()           { return pixQrCodeBase64; }
    public LocalDateTime getCriadoEm()           { return criadoEm; }
    public LocalDateTime getAtualizadoEm()       { return atualizadoEm; }

    // ── SETTERS ──────────────────────────────────────────────────────────────
    public void setEstabelecimento(Estabelecimento e)    { this.estabelecimento = e; }
    public void setAgendamento(Agendamento a)             { this.agendamento = a; }
    public void setMpPaymentId(String v)                  { this.mpPaymentId = v; }
    public void setValorTotal(BigDecimal v)               { this.valorTotal = v; }
    public void setTaxaPlataforma(BigDecimal v)           { this.taxaPlataforma = v; }
    public void setValorEstabelecimento(BigDecimal v)     { this.valorEstabelecimento = v; }
    public void setStatus(String v)                       { this.status = v; }
    public void setMetodoPagamento(String v)              { this.metodoPagamento = v; }
    public void setPixQrCode(String v)                    { this.pixQrCode = v; }
    public void setPixQrCodeBase64(String v)              { this.pixQrCodeBase64 = v; }
    public void setAtualizadoEm(LocalDateTime v)          { this.atualizadoEm = v; }
}
