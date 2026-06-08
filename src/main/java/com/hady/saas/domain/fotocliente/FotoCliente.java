package com.hady.saas.domain.fotocliente;

import com.hady.saas.domain.cliente.Cliente;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fotos_cliente")
public class FotoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoFoto tipo;

    @Column(nullable = false, length = 500)
    private String caminho;

    @Column(name = "nome_original", length = 255)
    private String nomeOriginal;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public enum TipoFoto {
        ANTES, DEPOIS
    }

    public FotoCliente() {}

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public TipoFoto getTipo() { return tipo; }
    public String getCaminho() { return caminho; }
    public String getNomeOriginal() { return nomeOriginal; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public void setId(Long id) { this.id = id; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public void setTipo(TipoFoto tipo) { this.tipo = tipo; }
    public void setCaminho(String caminho) { this.caminho = caminho; }
    public void setNomeOriginal(String nomeOriginal) { this.nomeOriginal = nomeOriginal; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
