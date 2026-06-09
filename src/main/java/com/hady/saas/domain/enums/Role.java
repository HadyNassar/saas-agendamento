package com.hady.saas.domain.enums;

public enum Role {

    // Acesso total. Máximo de 2 ADMINs por estabelecimento.
    ADMIN,

    // Acesso completo ao painel: agenda, clientes, serviços, horários, WhatsApp, relatórios e configurações.
    GERENTE,

    // Acesso restrito: vê apenas a própria agenda e serviços. Sem clientes, relatórios, configurações ou WhatsApp.
    PROFISSIONAL
}
