package com.hady.saas.domain.enums;

// Item (indicação/referral automatizada).
public enum StatusIndicacao {
    AGUARDANDO_USO,                  // link gerado, ninguém abriu ainda
    AGUARDANDO_PRIMEIRO_ATENDIMENTO, // indicado usou o link e agendou/foi identificado
    CONCLUIDA,                       // indicado concluiu o primeiro atendimento — cupons liberados
    EXPIRADA                         // passou dos 15 dias sem concluir
}