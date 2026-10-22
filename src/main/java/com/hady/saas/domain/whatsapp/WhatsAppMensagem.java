package com.hady.saas.domain.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_mensagens")
public class WhatsAppMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "telefone_destino", nullable = false, length = 20)
    private String telefoneDestino;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    // CONFIRMACAO, LEMBRETE_24H, LEMBRETE_2H, CANCELAMENTO, FALTA, RECEBIDA_TEXTO
    @Column(nullable = false, length = 30)
    private String tipo = "NOTIFICACAO";

    // PENDENTE, ENVIADO, ERRO, RECEBIDO
    @Column(nullable = false, length = 20)
    private String status = "PENDENTE";

    // ENVIADA ou RECEBIDA
    @Column(nullable = false, length = 10)
    private String direcao = "ENVIADA";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversa_id")
    private ConversaWhatsApp conversa;

    @Column(name = "erro_detalhe", columnDefinition = "TEXT")
    private String erroDetalhe;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public WhatsAppMensagem() {}

    // GETTERS
    public Long getId() { return id; }
    @JsonIgnore
    public Estabelecimento getEstabelecimento() { return estabelecimento; }
    @JsonIgnore
    public Agendamento getAgendamento() { return agendamento; }
    @JsonIgnore
    public Cliente getCliente() { return cliente; }
    public String getTelefoneDestino() { return telefoneDestino; }
    public String getMensagem() { return mensagem; }
    public String getTipo() { return tipo; }
    public String getStatus() { return status; }
    public String getDirecao() { return direcao; }
    @JsonIgnore
    public ConversaWhatsApp getConversa() { return conversa; }
    public String getErroDetalhe() { return erroDetalhe; }
    public LocalDateTime getEnviadoEm() { return enviadoEm; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    // SETTERS
    public void setEstabelecimento(Estabelecimento estabelecimento) { this.estabelecimento = estabelecimento; }
    public void setAgendamento(Agendamento agendamento) { this.agendamento = agendamento; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public void setTelefoneDestino(String telefoneDestino) { this.telefoneDestino = telefoneDestino; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setStatus(String status) { this.status = status; }
    public void setDirecao(String direcao) { this.direcao = direcao; }
    public void setConversa(ConversaWhatsApp conversa) { this.conversa = conversa; }
    public void setErroDetalhe(String erroDetalhe) { this.erroDetalhe = erroDetalhe; }
    public void setEnviadoEm(LocalDateTime enviadoEm) { this.enviadoEm = enviadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}