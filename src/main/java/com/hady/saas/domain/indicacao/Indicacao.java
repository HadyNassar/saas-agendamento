package com.hady.saas.domain.indicacao;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.cupom.CupomEstabelecimento;
import com.hady.saas.domain.enums.StatusIndicacao;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Item 17 — indicação/referral automatizada.
 *
 * Fluxo:
 *   1. Cliente A (indicador) recebe um link único (código) por WhatsApp, junto
 *      com a mensagem de agradecimento por nota alta (ver WhatsAppService.
 *      enviarAgradecimentoNotaAlta). Nasce AGUARDANDO_USO, expira em 15 dias
 *      (dataExpiracao = criadoEm + 15 dias, regra confirmada com o dono).
 *   2. Cliente B (indicado) abre o link (GET /app/{slug}/indicar/{codigo}) —
 *      é identificado/criado como Cliente e vinculado aqui. Status vira
 *      AGUARDANDO_PRIMEIRO_ATENDIMENTO. Link não pode ser reaproveitado por
 *      outro indicado a partir desse ponto.
 *   3. Quando o primeiro agendamento do indicado é concluído
 *      (AgendamentoService.concluir), a indicação vira CONCLUIDA e DOIS
 *      cupons pessoais de uso único são gerados — um para o indicador, um
 *      para o indicado (regra confirmada: os dois ganham).
 *   4. Job de expiração (mesmo padrão de PagamentoPendenteCleanupJob) marca
 *      como EXPIRADA quem passar de 15 dias ainda em AGUARDANDO_USO ou
 *      AGUARDANDO_PRIMEIRO_ATENDIMENTO sem concluir.
 */
@Entity
@Table(name = "indicacoes")
public class Indicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indicador_id", nullable = false)
    private Cliente indicador;

    // Nulo até o link ser aberto/usado por alguém
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indicado_id")
    private Cliente indicado;

    // Código único do link (path /app/{slug}/indicar/{codigo})
    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    // Agendamento do indicado cuja conclusão libera os cupons — setado quando
    // o indicado agenda pela primeira vez após usar o link, não antes.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_referencia_id")
    private Agendamento agendamentoReferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cupom_indicador_id")
    private CupomEstabelecimento cupomIndicador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cupom_indicado_id")
    private CupomEstabelecimento cupomIndicado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusIndicacao status = StatusIndicacao.AGUARDANDO_USO;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Indicacao() {}

    // ── GETTERS ──────────────────────────────────────────────────────────────
    public Long getId()                             { return id; }
    public Estabelecimento getEstabelecimento()     { return estabelecimento; }
    public Cliente getIndicador()                   { return indicador; }
    public Cliente getIndicado()                    { return indicado; }
    public String getCodigo()                       { return codigo; }
    public Agendamento getAgendamentoReferencia()   { return agendamentoReferencia; }
    public CupomEstabelecimento getCupomIndicador() { return cupomIndicador; }
    public CupomEstabelecimento getCupomIndicado()  { return cupomIndicado; }
    public StatusIndicacao getStatus()              { return status; }
    public LocalDateTime getDataExpiracao()         { return dataExpiracao; }
    public LocalDateTime getUsadoEm()               { return usadoEm; }
    public LocalDateTime getConcluidaEm()           { return concluidaEm; }
    public LocalDateTime getCriadoEm()              { return criadoEm; }

    // ── SETTERS ──────────────────────────────────────────────────────────────
    public void setEstabelecimento(Estabelecimento e)                 { this.estabelecimento = e; }
    public void setIndicador(Cliente indicador)                       { this.indicador = indicador; }
    public void setIndicado(Cliente indicado)                         { this.indicado = indicado; }
    public void setCodigo(String codigo)                              { this.codigo = codigo; }
    public void setAgendamentoReferencia(Agendamento a)               { this.agendamentoReferencia = a; }
    public void setCupomIndicador(CupomEstabelecimento c)             { this.cupomIndicador = c; }
    public void setCupomIndicado(CupomEstabelecimento c)              { this.cupomIndicado = c; }
    public void setStatus(StatusIndicacao status)                     { this.status = status; }
    public void setDataExpiracao(LocalDateTime dataExpiracao)         { this.dataExpiracao = dataExpiracao; }
    public void setUsadoEm(LocalDateTime usadoEm)                     { this.usadoEm = usadoEm; }
    public void setConcluidaEm(LocalDateTime concluidaEm)             { this.concluidaEm = concluidaEm; }
}