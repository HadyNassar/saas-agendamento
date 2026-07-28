package com.hady.saas.domain.agendamento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.cupom.CupomEstabelecimento;
import com.hady.saas.domain.enums.StatusAgendamento;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.servico.Servico;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(nullable = false)
    private LocalDateTime dataHoraFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    private String observacoes;
    private boolean agendadoPeloCliente = false;

    // Controla se o WhatsApp de avaliação já foi enviado para este agendamento
    @Column(nullable = false)
    private boolean avaliacaoEnviada = false;

    // Item 2 (camada ELITE): true quando o cliente respondeu "1" (confirmar)
    // ao lembrete de 24h/2h pelo bot conversacional. Não confundir com o status
    // AGENDADO — este campo é só um registro de que o cliente reagiu de fato,
    // útil futuramente para a métrica de "taxa de confirmação" (item 12, PLATINUM).
    @Column(name = "confirmado_pelo_cliente", nullable = false)
    private boolean confirmadoPeloCliente = false;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    // Migration V25 (seção 5-B) — cupom do próprio estabelecimento aplicado
    // via link público neste agendamento, se houver. Só REGISTRO/relatório
    // (item 12 futuro); não afeta o cálculo de sinal/pagamento via Mercado
    // Pago, que continua um fluxo separado e não foi tocado aqui.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cupom_estabelecimento_id")
    private CupomEstabelecimento cupomEstabelecimento;

    @Column(name = "valor_desconto_aplicado", precision = 10, scale = 2)
    private BigDecimal valorDescontoAplicado;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Agendamento() {}

    public Long getId()                          { return id; }
    public Cliente getCliente()                  { return cliente; }
    public Profissional getProfissional()        { return profissional; }
    public Servico getServico()                  { return servico; }
    public Estabelecimento getEstabelecimento()  { return estabelecimento; }
    public LocalDateTime getDataHoraInicio()     { return dataHoraInicio; }
    public LocalDateTime getDataHoraFim()        { return dataHoraFim; }
    public StatusAgendamento getStatus()         { return status; }
    public String getObservacoes()               { return observacoes; }
    public boolean isAgendadoPeloCliente()       { return agendadoPeloCliente; }
    public boolean isAvaliacaoEnviada()          { return avaliacaoEnviada; }
    public boolean isConfirmadoPeloCliente()     { return confirmadoPeloCliente; }
    public LocalDateTime getConfirmadoEm()       { return confirmadoEm; }
    public CupomEstabelecimento getCupomEstabelecimento() { return cupomEstabelecimento; }
    public BigDecimal getValorDescontoAplicado() { return valorDescontoAplicado; }
    public LocalDateTime getCriadoEm()           { return criadoEm; }

    public void setId(Long id)                                        { this.id = id; }
    public void setCliente(Cliente cliente)                           { this.cliente = cliente; }
    public void setProfissional(Profissional profissional)            { this.profissional = profissional; }
    public void setServico(Servico servico)                           { this.servico = servico; }
    public void setEstabelecimento(Estabelecimento estabelecimento)   { this.estabelecimento = estabelecimento; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio)      { this.dataHoraInicio = dataHoraInicio; }
    public void setDataHoraFim(LocalDateTime dataHoraFim)            { this.dataHoraFim = dataHoraFim; }
    public void setStatus(StatusAgendamento status)                   { this.status = status; }
    public void setObservacoes(String observacoes)                    { this.observacoes = observacoes; }
    public void setAgendadoPeloCliente(boolean agendadoPeloCliente)  { this.agendadoPeloCliente = agendadoPeloCliente; }
    public void setAvaliacaoEnviada(boolean avaliacaoEnviada)        { this.avaliacaoEnviada = avaliacaoEnviada; }
    public void setConfirmadoPeloCliente(boolean confirmadoPeloCliente) { this.confirmadoPeloCliente = confirmadoPeloCliente; }
    public void setConfirmadoEm(LocalDateTime confirmadoEm)          { this.confirmadoEm = confirmadoEm; }
    public void setCupomEstabelecimento(CupomEstabelecimento cupomEstabelecimento) { this.cupomEstabelecimento = cupomEstabelecimento; }
    public void setValorDescontoAplicado(BigDecimal valorDescontoAplicado) { this.valorDescontoAplicado = valorDescontoAplicado; }
    public void setCriadoEm(LocalDateTime criadoEm)                  { this.criadoEm = criadoEm; }
}