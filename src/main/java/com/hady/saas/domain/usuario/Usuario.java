package com.hady.saas.domain.usuario;

import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    // ADMIN     → acesso total (todos os planos)
    // FUNCIONARIO     → acesso total (BASE e PREMIUM)
    // FUNCIONARIO_TOP → acesso restrito (apenas plano TOP)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    // Construtor vazio obrigatório para JPA
    public Usuario() {}

    // GETTERS
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public String getNome() { return nome; }
    public Role getRole() { return role; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setSenha(String senha) { this.senha = senha; }
    public void setNome(String nome) { this.nome = nome; }
    public void setRole(Role role) { this.role = role; }
    public void setEstabelecimento(Estabelecimento estabelecimento) { this.estabelecimento = estabelecimento; }
}
