package com.hady.saas.domain.enums;

public enum EstagioCliente {
    NOVO,
    RECORRENTE,
    VIP,
    INATIVO,

    // Pseudo-segmento usado SOMENTE como filtro de campanha de disparo em
    // massa (CampanhaEnvioService) — nunca é um valor real de
    // Cliente.estagioAtual. Significa "todos os clientes cadastrados no
    // estabelecimento, independente de estágio calculado".
    TODOS
}