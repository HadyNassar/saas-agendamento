package com.hady.saas.domain.enums;

import java.math.BigDecimal;

public enum Plano {

    STANDARD    (2, new BigDecimal("49.00"),  "Agendara Standard", 1),
    ELITE       (4, new BigDecimal("69.00"),  "Agendara Elite", 1),
    PLATINUM    (7, new BigDecimal("119.00"), "Agendara Platinum", 1),
    // Promoção de lançamento — pagamento único via PIX, válido para os
    // primeiros clientes. Mesmo limite de usuários do Elite mensal.
    ELITE_ANUAL (4, new BigDecimal("150.00"), "Agendara Elite Anual (Promoção)", 12),
    // Plano exclusivo (sob medida) para clínica com ~18 profissionais.
    // Mesmas funções do Elite (nenhum gating usa == ELITE; tudo é != STANDARD),
    // porém com 20 vagas de funcionário (GERENTE/PROFISSIONAL; ADMIN não conta).
    // Atribuído MANUALMENTE pelo painel admin — não é vendido no front nem tem
    // plano recorrente no Mercado Pago. O preço abaixo é só referência.
    ELITE_CLINICA (20, new BigDecimal("0.00"), "Agendara Elite Clínica (exclusivo)", 1);

    private final int limiteUsuarios;
    private final BigDecimal preco;
    private final String descricao;
    private final int duracaoMeses;

    Plano(int limiteUsuarios, BigDecimal preco, String descricao, int duracaoMeses) {
        this.limiteUsuarios = limiteUsuarios;
        this.preco          = preco;
        this.descricao      = descricao;
        this.duracaoMeses   = duracaoMeses;
    }

    public int getLimiteUsuarios() { return limiteUsuarios; }
    public BigDecimal getPreco()   { return preco; }
    public String getDescricao()   { return descricao; }

    /** Quantos meses de acesso esse plano libera por ciclo de pagamento. */
    public int getDuracaoMeses()   { return duracaoMeses; }
}