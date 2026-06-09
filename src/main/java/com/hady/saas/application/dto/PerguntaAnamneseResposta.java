package com.hady.saas.application.dto;

import com.hady.saas.domain.anamnese.PerguntaAnamnese;

public class PerguntaAnamneseResposta {

    private Long id;
    private String texto;
    private String tipo;
    private int ordem;
    private boolean ativa;

    public PerguntaAnamneseResposta(PerguntaAnamnese p) {
        this.id = p.getId();
        this.texto = p.getTexto();
        this.tipo = p.getTipo();
        this.ordem = p.getOrdem();
        this.ativa = p.isAtiva();
    }

    // GETTERS
    public Long getId() { return id; }
    public String getTexto() { return texto; }
    public String getTipo() { return tipo; }
    public int getOrdem() { return ordem; }
    public boolean isAtiva() { return ativa; }
}