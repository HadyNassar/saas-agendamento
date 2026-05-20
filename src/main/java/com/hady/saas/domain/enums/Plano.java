package com.hady.saas.domain.enums;

public enum Plano {

    STANDARD(2),
    ELITE(4),
    PLATINUM(7);

    private final int limiteUsuarios;

    Plano(int limiteUsuarios) {
        this.limiteUsuarios = limiteUsuarios;
    }

    public int getLimiteUsuarios() {
        return limiteUsuarios;
    }
}