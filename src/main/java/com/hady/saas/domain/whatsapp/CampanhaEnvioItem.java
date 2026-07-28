package com.hady.saas.domain.whatsapp;

import com.hady.saas.domain.cliente.Cliente;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Um item da fila de disparo em massa: UM destinatário de UMA CampanhaEnvio.
 * É a unidade real de trabalho que CampanhaEnvioProcessorJob processa —
 * a CampanhaEnvio nunca é "enviada" de uma vez só, ela só existe como
 * agregado de progresso sobre estes itens.
 *
 * Ciclo de vida do status: PENDENTE -> (ENVIADO | ERRO). Sem retry automático
 * nesta primeira versão — um item em ERRO fica assim; retry manual pode ser
 * adicionado depois se o volume de falhas justificar (não implementado
 * porque não foi pedido).
 */
@Entity
@Table(name = "campanha_envio_item")
public class CampanhaEnvioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campanha_id", nullable = false)
    private CampanhaEnvio campanha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "telefone_destino", nullable = false, length = 20)
    private String telefoneDestino;

    @Column(nullable = false, length = 20)
    private String status = "PENDENTE";

    @Column(name = "erro_detalhe", columnDefinition = "TEXT")
    private String erroDetalhe;

    @Column(name = "processado_em")
    private LocalDateTime processadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public CampanhaEnvioItem() {}

    public CampanhaEnvioItem(CampanhaEnvio campanha, Cliente cliente, String telefoneDestino) {
        this.campanha = campanha;
        this.cliente = cliente;
        this.telefoneDestino = telefoneDestino;
    }

    public Long getId()                         { return id; }
    public CampanhaEnvio getCampanha()           { return campanha; }
    public Cliente getCliente()                  { return cliente; }
    public String getTelefoneDestino()           { return telefoneDestino; }
    public String getStatus()                    { return status; }
    public String getErroDetalhe()               { return erroDetalhe; }
    public LocalDateTime getProcessadoEm()       { return processadoEm; }
    public LocalDateTime getCriadoEm()            { return criadoEm; }

    public void setId(Long id)                                 { this.id = id; }
    public void setCampanha(CampanhaEnvio campanha)            { this.campanha = campanha; }
    public void setCliente(Cliente cliente)                    { this.cliente = cliente; }
    public void setTelefoneDestino(String telefoneDestino)     { this.telefoneDestino = telefoneDestino; }
    public void setStatus(String status)                       { this.status = status; }
    public void setErroDetalhe(String erroDetalhe)             { this.erroDetalhe = erroDetalhe; }
    public void setProcessadoEm(LocalDateTime processadoEm)    { this.processadoEm = processadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm)            { this.criadoEm = criadoEm; }
}