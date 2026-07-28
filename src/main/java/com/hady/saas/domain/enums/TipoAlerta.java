package com.hady.saas.domain.enums;

// Tipos de alerta exibidos no painel ao abrir (badge/lista de pendências).
// Genérico por design — item 16 (gatilho de reputação) usa AVALIACAO_NOTA_BAIXA,
// mas outros fluxos podem reaproveitar essa mesma tabela no futuro.
public enum TipoAlerta {
    AVALIACAO_NOTA_BAIXA // avaliação pós-atendimento com nota 1 ou 2
}