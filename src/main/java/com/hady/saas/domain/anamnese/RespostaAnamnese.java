package com.hady.saas.domain.anamnese;

import com.hady.saas.domain.cliente.Cliente;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "respostas_anamnese")
public class RespostaAnamnese {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pergunta_id", nullable = false)
    private PerguntaAnamnese pergunta;

    // Para SIM_NAO e SIM_NAO_DETALHE: "SIM" ou "NAO"
    // Para TEXTO_LIVRE: a resposta digitada
    @Column(nullable = false)
    private String resposta;

    // Para SIM_NAO_DETALHE: o detalhe quando a resposta é SIM
    private String detalhe;

    // Indica se foi preenchido pelo cliente (true) ou editado pelo usuário (false)
    private boolean preenchidoPeloCliente = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime preenchidoEm = LocalDateTime.now();

    // Última edição feita pelo usuário interno
    private LocalDateTime editadoEm;

    public RespostaAnamnese() {}

    // GETTERS
    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public PerguntaAnamnese getPergunta() { return pergunta; }
    public String getResposta() { return resposta; }
    public String getDetalhe() { return detalhe; }
    public boolean isPreenchidoPeloCliente() { return preenchidoPeloCliente; }
    public LocalDateTime getPreenchidoEm() { return preenchidoEm; }
    public LocalDateTime getEditadoEm() { return editadoEm; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public void setPergunta(PerguntaAnamnese pergunta) { this.pergunta = pergunta; }
    public void setResposta(String resposta) { this.resposta = resposta; }
    public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
    public void setPreenchidoPeloCliente(boolean preenchidoPeloCliente) { this.preenchidoPeloCliente = preenchidoPeloCliente; }
    public void setPreenchidoEm(LocalDateTime preenchidoEm) { this.preenchidoEm = preenchidoEm; }
    public void setEditadoEm(LocalDateTime editadoEm) { this.editadoEm = editadoEm; }
}
