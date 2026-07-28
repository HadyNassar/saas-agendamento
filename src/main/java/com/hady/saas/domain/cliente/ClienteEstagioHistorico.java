package com.hady.saas.domain.cliente;

import com.hady.saas.domain.enums.EstagioCliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro histórico de UMA transição de EstagioCliente (ex: RECORRENTE -> VIP).
 *
 * Existe para dar suporte futuro aos itens 10 (reativação inteligente por
 * regras) e 11 (funil/CRM via WhatsApp), que precisam disparar ação no
 * MOMENTO em que um cliente muda de estágio — não só saber qual é o estágio
 * atual. Nenhum consumidor desses itens foi implementado ainda; esta tabela
 * só grava o fato histórico, pronta para ser lida quando esses itens forem
 * construídos.
 *
 * Uma linha é gravada em ClienteSegmentacaoService.recalcularEPersistir()
 * sempre (e somente) que o estágio efetivamente muda em relação ao valor já
 * salvo em Cliente.estagioAtual.
 */
@Entity
@Table(name = "cliente_estagio_historico")
public class ClienteEstagioHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estagio_anterior")
    private EstagioCliente estagioAnterior; // null na primeira classificação do cliente

    @Enumerated(EnumType.STRING)
    @Column(name = "estagio_novo", nullable = false)
    private EstagioCliente estagioNovo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    // Migration V25. Marca se esta transição já gerou (ou foi avaliada para
    // gerar) uma CampanhaEnvio automática — ver CampanhaAutomaticaService.
    // Evita reprocessar a mesma linha em execuções seguintes do job diário.
    // Default true para linhas antigas via migration (nada a reprocessar
    // retroativamente); novas linhas nascem false.
    @Column(name = "processado_campanha_automatica", nullable = false)
    private boolean processadoCampanhaAutomatica = false;

    public ClienteEstagioHistorico() {}

    public ClienteEstagioHistorico(Cliente cliente, Estabelecimento estabelecimento,
                                   EstagioCliente estagioAnterior, EstagioCliente estagioNovo) {
        this.cliente = cliente;
        this.estabelecimento = estabelecimento;
        this.estagioAnterior = estagioAnterior;
        this.estagioNovo = estagioNovo;
    }

    public Long getId()                             { return id; }
    public Cliente getCliente()                      { return cliente; }
    public Estabelecimento getEstabelecimento()      { return estabelecimento; }
    public EstagioCliente getEstagioAnterior()       { return estagioAnterior; }
    public EstagioCliente getEstagioNovo()            { return estagioNovo; }
    public LocalDateTime getCriadoEm()                { return criadoEm; }
    public boolean isProcessadoCampanhaAutomatica()   { return processadoCampanhaAutomatica; }

    public void setId(Long id)                                       { this.id = id; }
    public void setCliente(Cliente cliente)                          { this.cliente = cliente; }
    public void setEstabelecimento(Estabelecimento estabelecimento)  { this.estabelecimento = estabelecimento; }
    public void setEstagioAnterior(EstagioCliente estagioAnterior)   { this.estagioAnterior = estagioAnterior; }
    public void setEstagioNovo(EstagioCliente estagioNovo)            { this.estagioNovo = estagioNovo; }
    public void setCriadoEm(LocalDateTime criadoEm)                  { this.criadoEm = criadoEm; }
    public void setProcessadoCampanhaAutomatica(boolean processadoCampanhaAutomatica) {
        this.processadoCampanhaAutomatica = processadoCampanhaAutomatica;
    }
}