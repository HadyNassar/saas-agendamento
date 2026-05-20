package com.hady.saas.domain.profissional;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.usuario.Usuario;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profissionais")
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String telefone;
    private String fotoPerfil; // URL da foto (futuro)

    // Cada profissional pertence a um estabelecimento (multi-tenant)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    // Profissional pode ter login — vínculo com Usuario (opcional)
    // Null = profissional sem acesso ao sistema
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    // Especialidades do profissional (ex: corte, barba, sobrancelha)
    // Um profissional pode ter várias especialidades
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "profissional_especialidades",
        joinColumns = @JoinColumn(name = "profissional_id"),
        inverseJoinColumns = @JoinColumn(name = "especialidade_id")
    )
    private List<Especialidade> especialidades = new ArrayList<>();

    private boolean ativo = true;

    // Construtor vazio obrigatório para JPA
    public Profissional() {}

    // GETTERS
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getFotoPerfil() { return fotoPerfil; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }
    public Usuario getUsuario() { return usuario; }
    public List<Especialidade> getEspecialidades() { return especialidades; }
    public boolean isAtivo() { return ativo; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
    public void setEstabelecimento(Estabelecimento estabelecimento) { this.estabelecimento = estabelecimento; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public void setEspecialidades(List<Especialidade> especialidades) { this.especialidades = especialidades; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
