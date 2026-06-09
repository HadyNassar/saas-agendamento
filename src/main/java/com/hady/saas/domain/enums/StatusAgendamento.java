package com.hady.saas.domain.enums;

public enum StatusAgendamento {
    AGENDADO,             // agendamento confirmado
    AGUARDANDO_PAGAMENTO, // aguardando pagamento do sinal para confirmar
    CONCLUIDO,            // atendimento realizado
    CANCELADO,            // cancelado pelo estabelecimento ou cliente
    FALTOU                // cliente não compareceu
}