package com.hady.saas.domain.enums;

public enum Role {

    // Existe em todos os planos — acesso total sempre
    ADMIN,

    // Existe em STANDARD e ELITE — acesso total igual ao ADMIN
    FUNCIONARIO,

    // Existe apenas no PLATINUM — acesso restrito (vê só a própria agenda)
    FUNCIONARIO_PLATINUM
}
