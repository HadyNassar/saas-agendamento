package com.hady.saas.domain.enums;

public enum Role {

    // Dono do SaaS — acesso ao painel administrativo master, fora do
    // escopo de qualquer estabelecimento. Não vinculado a nenhuma conta
    // de salão (estabelecimento = null).
    ADMIN_MASTER,

    // Acesso total. Máximo de 2 ADMINs por estabelecimento.
    ADMIN,

    // Acesso completo ao painel: agenda, clientes, serviços, horários, WhatsApp, relatórios e configurações.
    GERENTE,

    // Acesso restrito: vê apenas a própria agenda e serviços. Sem clientes, relatórios, configurações ou WhatsApp.
    PROFISSIONAL
}