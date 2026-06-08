package com.hady.saas.domain.usuario;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime expiraEm;

    @Column(nullable = false)
    private boolean usado = false;

    public PasswordResetToken() {}

    public PasswordResetToken(String token, Usuario usuario, LocalDateTime expiraEm) {
        this.token = token;
        this.usuario = usuario;
        this.expiraEm = expiraEm;
    }

    public boolean estaExpirado() {
        return LocalDateTime.now().isAfter(expiraEm);
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public boolean isUsado() { return usado; }

    public void setUsado(boolean usado) { this.usado = usado; }
}