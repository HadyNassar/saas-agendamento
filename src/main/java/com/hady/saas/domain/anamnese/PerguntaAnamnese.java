package com.hady.saas.domain.anamnese;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

@Entity
@Table(name = "perguntas_anamnese")
public class PerguntaAnamnese {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String texto; // ex: "Tem alergia a algum produto?"

    @Column(nullable = false)
    private String tipo; // SIM_NAO, SIM_NAO_DETALHE, TEXTO_LIVRE

    private int ordem = 0; // ordem de exibição

    private boolean ativa = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    public PerguntaAnamnese() {}

    // GETTERS
    public Long getId() { return id; }
    public String getTexto() { return texto; }
    public String getTipo() { return tipo; }
    public int getOrdem() { return ordem; }
    public boolean isAtiva() { return ativa; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setTexto(String texto) { this.texto = texto; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public void setEstabelecimento(Estabelecimento estabelecimento) { this.estabelecimento = estabelecimento; }
}
