package com.hady.saas.domain.alerta;

import com.hady.saas.domain.enums.TipoAlerta;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// Tabela genérica de alertas exibidos ao dono ao abrir o painel.
// Item 16 (gatilho de reputação) é o primeiro uso: nota baixa (1-2) na
// avaliação pós-atendimento gera um registro aqui em vez de mensagem de
// WhatsApp para o dono. Desenhada para ser reaproveitada por outros tipos
// de alerta futuramente (ver TipoAlerta).
@Entity
@Table(name = "alertas_painel")
public class AlertaPainel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoAlerta tipo;

    // Texto curto já pronto para exibir na lista de alertas do painel
    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    // Id da entidade de origem (ex.: id da AvaliacaoAtendimento), para permitir
    // deep-link do alerta até o registro original. Sem FK física de propósito —
    // tipo de origem varia conforme TipoAlerta.
    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(nullable = false)
    private boolean visto = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public AlertaPainel() {}

    // ── GETTERS ──────────────────────────────────────────────────────────────
    public Long getId()                         { return id; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }
    public TipoAlerta getTipo()                 { return tipo; }
    public String getMensagem()                 { return mensagem; }
    public Long getReferenciaId()               { return referenciaId; }
    public boolean isVisto()                    { return visto; }
    public LocalDateTime getCriadoEm()          { return criadoEm; }

    // ── SETTERS ──────────────────────────────────────────────────────────────
    public void setEstabelecimento(Estabelecimento e) { this.estabelecimento = e; }
    public void setTipo(TipoAlerta tipo)              { this.tipo = tipo; }
    public void setMensagem(String mensagem)          { this.mensagem = mensagem; }
    public void setReferenciaId(Long referenciaId)    { this.referenciaId = referenciaId; }
    public void setVisto(boolean visto)               { this.visto = visto; }
}