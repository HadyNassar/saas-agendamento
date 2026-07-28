package com.hady.saas.domain.whatsapp;

import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversa_whatsapp",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cliente_id", "estabelecimento_id"}))
public class ConversaWhatsApp {

    // Estados possíveis (string livre, mesmo padrão de WhatsAppMensagem.tipo):
    // LIVRE, AGUARDANDO_SERVICO, AGUARDANDO_PROFISSIONAL, AGUARDANDO_DATA,
    // AGUARDANDO_HORARIO, CONFIRMANDO (fluxo do bot de agendamento — item 1),
    // AGUARDANDO_CONFIRMACAO_LEMBRETE (item 2 — cliente respondendo 1/2 a um
    // lembrete de 24h/2h; tem prioridade sobre qualquer fluxo de agendamento
    // em andamento, ver BotAgendamentoService.processarMensagem),
    // AGUARDANDO_ESCOLHA_AGENDAMENTO e AGUARDANDO_NOVO_HORARIO (item 3 —
    // reagendamento self-service; ver BotAgendamentoService para o fluxo completo)
    public static final String ESTADO_LIVRE = "LIVRE";
    public static final String ESTADO_AGUARDANDO_CONFIRMACAO_LEMBRETE = "AGUARDANDO_CONFIRMACAO_LEMBRETE";
    public static final String ESTADO_AGUARDANDO_ESCOLHA_AGENDAMENTO = "AGUARDANDO_ESCOLHA_AGENDAMENTO";
    public static final String ESTADO_AGUARDANDO_NOVO_HORARIO = "AGUARDANDO_NOVO_HORARIO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @Column(nullable = false, length = 30)
    private String estado = ESTADO_LIVRE;

    // Contexto do fluxo em andamento (ex: {"servicoId":12,"profissionalId":3}),
    // guardado como texto simples — sem precisar de tabela nova agora
    @Column(columnDefinition = "TEXT")
    private String contexto;

    @Column(name = "robot_pausado", nullable = false)
    private boolean robotPausado = false;

    @Column(name = "pausado_ate")
    private LocalDateTime pausadoAte;

    @Column(name = "ultima_mensagem_em")
    private LocalDateTime ultimaMensagemEm;

    @Column(name = "ultima_mensagem_preview", length = 200)
    private String ultimaMensagemPreview;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public ConversaWhatsApp() {}

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    /**
     * Robô está pausado agora, considerando a expiração automática de pausadoAte.
     * Não precisa de job em background: se pausadoAte já passou, trata como não-pausado
     * (e quem chamar isso deveria também persistir o "despause" via limparPausaSeExpirada()).
     */
    public boolean isRobotPausadoAgora() {
        if (!robotPausado) return false;
        if (pausadoAte == null) return true; // pausa manual sem prazo
        return pausadoAte.isAfter(LocalDateTime.now());
    }

    /** Chamar antes de decidir se o bot responde: limpa a pausa se o prazo já passou. */
    public void limparPausaSeExpirada() {
        if (robotPausado && pausadoAte != null && !pausadoAte.isAfter(LocalDateTime.now())) {
            this.robotPausado = false;
            this.pausadoAte = null;
        }
    }

    // GETTERS
    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }
    public String getEstado() { return estado; }
    public String getContexto() { return contexto; }
    public boolean isRobotPausado() { return robotPausado; }
    public LocalDateTime getPausadoAte() { return pausadoAte; }
    public LocalDateTime getUltimaMensagemEm() { return ultimaMensagemEm; }
    public String getUltimaMensagemPreview() { return ultimaMensagemPreview; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }

    // SETTERS
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public void setEstabelecimento(Estabelecimento estabelecimento) { this.estabelecimento = estabelecimento; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setContexto(String contexto) { this.contexto = contexto; }
    public void setRobotPausado(boolean robotPausado) { this.robotPausado = robotPausado; }
    public void setPausadoAte(LocalDateTime pausadoAte) { this.pausadoAte = pausadoAte; }
    public void setUltimaMensagemEm(LocalDateTime ultimaMensagemEm) { this.ultimaMensagemEm = ultimaMensagemEm; }
    public void setUltimaMensagemPreview(String ultimaMensagemPreview) { this.ultimaMensagemPreview = ultimaMensagemPreview; }
}