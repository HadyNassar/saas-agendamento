package com.hady.saas.domain.enums;

import java.math.BigDecimal;

public enum Plano {

    STANDARD(2, new BigDecimal("2.00"),  "Agendara Standard"),
    ELITE   (4, new BigDecimal("69.00"),  "Agendara Elite"),
    PLATINUM (7, new BigDecimal("119.00"), "Agendara Platinum");

    private final int limiteUsuarios;
    private final BigDecimal preco;
    private final String descricao;

    Plano(int limiteUsuarios, BigDecimal preco, String descricao) {
        this.limiteUsuarios = limiteUsuarios;
        this.preco          = preco;
        this.descricao      = descricao;
    }

    public int getLimiteUsuarios() { return limiteUsuarios; }
    public BigDecimal getPreco()   { return preco; }
    public String getDescricao()   { return descricao; }
}