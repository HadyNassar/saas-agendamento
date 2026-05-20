package com.hady.saas.domain.profissional;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

@Entity
@Table(name = "especialidades")
public class Especialidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome; // ex: Corte Masculino, Barba, Sobrancelha, Hidratação

    // Especialidades pertencem ao estabelecimento (multi-tenant)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    // Construtor vazio obrigatório para JPA
    public Especialidade() {}

    // GETTERS
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setEstabelecimento(Estabelecimento estabelecimento) { this.estabelecimento = estabelecimento; }
}
