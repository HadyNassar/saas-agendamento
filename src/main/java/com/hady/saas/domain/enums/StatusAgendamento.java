package com.hady.saas.domain.enums;

public enum StatusAgendamento {
    AGENDADO,   // agendamento confirmado
    CONCLUIDO,  // atendimento realizado
    CANCELADO,  // cancelado pelo estabelecimento ou cliente
    FALTOU      // cliente não compareceu
}
