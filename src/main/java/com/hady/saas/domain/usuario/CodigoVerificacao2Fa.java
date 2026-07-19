package com.hady.saas.domain.usuario;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigo_verificacao_2fa")
public class CodigoVerificacao2FA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    public CodigoVerificacao2FA() {}

    public CodigoVerificacao2FA(Long usuarioId, String codigo, LocalDateTime expiraEm) {
        this.usuarioId = usuarioId;
        this.codigo = codigo;
        this.expiraEm = expiraEm;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId()                              { return id; }
    public Long getUsuarioId()                       { return usuarioId; }
    public String getCodigo()                        { return codigo; }
    public boolean isUsado()                         { return usado; }
    public LocalDateTime getExpiraEm()               { return expiraEm; }
    public LocalDateTime getCriadoEm()                { return criadoEm; }

    public void setId(Long id)                                   { this.id = id; }
    public void setUsuarioId(Long usuarioId)                     { this.usuarioId = usuarioId; }
    public void setCodigo(String codigo)                         { this.codigo = codigo; }
    public void setUsado(boolean usado)                          { this.usado = usado; }
    public void setExpiraEm(LocalDateTime expiraEm)              { this.expiraEm = expiraEm; }
    public void setCriadoEm(LocalDateTime criadoEm)              { this.criadoEm = criadoEm; }
}