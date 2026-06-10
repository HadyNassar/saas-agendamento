package com.hady.saas.domain.oauth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "oauth_state")
public class OAuthState {

    @Id
    private String state;

    @Column(name = "estabelecimento_id", nullable = false)
    private Long estabelecimentoId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public OAuthState() {}

    public OAuthState(String state, Long estabelecimentoId, Instant criadoEm) {
        this.state = state;
        this.estabelecimentoId = estabelecimentoId;
        this.criadoEm = criadoEm;
    }

    public String getState() { return state; }
    public Long getEstabelecimentoId() { return estabelecimentoId; }
    public Instant getCriadoEm() { return criadoEm; }
}