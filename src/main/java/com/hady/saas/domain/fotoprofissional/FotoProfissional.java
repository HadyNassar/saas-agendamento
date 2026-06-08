package com.hady.saas.domain.fotoprofissional;

import com.hady.saas.domain.profissional.Profissional;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fotos_profissional")
public class FotoProfissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @Column(nullable = false, length = 500)
    private String caminho;

    @Column(name = "nome_original", length = 255)
    private String nomeOriginal;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public FotoProfissional() {}

    public Long getId() { return id; }
    public Profissional getProfissional() { return profissional; }
    public String getCaminho() { return caminho; }
    public String getNomeOriginal() { return nomeOriginal; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public void setId(Long id) { this.id = id; }
    public void setProfissional(Profissional profissional) { this.profissional = profissional; }
    public void setCaminho(String caminho) { this.caminho = caminho; }
    public void setNomeOriginal(String nomeOriginal) { this.nomeOriginal = nomeOriginal; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
