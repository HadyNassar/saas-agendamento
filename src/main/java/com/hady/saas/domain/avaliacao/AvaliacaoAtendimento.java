package com.hady.saas.domain.avaliacao;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacoes_atendimento")
public class AvaliacaoAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", nullable = false, unique = true)
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    // Nota de 1 a 5
    @Column(nullable = false)
    private Integer nota;

    // Comentário opcional
    @Column(columnDefinition = "TEXT")
    private String comentario;

    // Oculta a avaliação da página pública (controle anti-fake)
    @Column(nullable = false)
    private boolean oculta = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public AvaliacaoAtendimento() {}

    // ── GETTERS ──────────────────────────────────────────────────────────────
    public Long getId()                         { return id; }
    public Agendamento getAgendamento()         { return agendamento; }
    public Cliente getCliente()                 { return cliente; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }
    public Integer getNota()                    { return nota; }
    public String getComentario()               { return comentario; }
    public boolean isOculta()                   { return oculta; }
    public LocalDateTime getCriadoEm()          { return criadoEm; }

    // ── SETTERS ──────────────────────────────────────────────────────────────
    public void setAgendamento(Agendamento agendamento)           { this.agendamento = agendamento; }
    public void setCliente(Cliente cliente)                       { this.cliente = cliente; }
    public void setEstabelecimento(Estabelecimento e)             { this.estabelecimento = e; }
    public void setNota(Integer nota)                             { this.nota = nota; }
    public void setComentario(String comentario)                  { this.comentario = comentario; }
    public void setOculta(boolean oculta)                         { this.oculta = oculta; }
}