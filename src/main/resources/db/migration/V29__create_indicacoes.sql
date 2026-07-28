-- Item 16 (gatilho de reputação) — tabela genérica de alertas do painel.
-- Primeiro uso: alerta de avaliação com nota baixa (1-2). Desenhada para ser
-- reaproveitada por outros tipos de alerta futuramente (coluna `tipo`).

CREATE TABLE alertas_painel (
                                id                  BIGSERIAL PRIMARY KEY,
                                estabelecimento_id  BIGINT NOT NULL REFERENCES estabelecimentos(id),
                                tipo                VARCHAR(40) NOT NULL,
                                mensagem            TEXT NOT NULL,
                                referencia_id       BIGINT,
                                visto               BOOLEAN NOT NULL DEFAULT FALSE,
                                criado_em           TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Consulta mais comum: alertas não vistos de um estabelecimento, mais recentes primeiro
CREATE INDEX idx_alertas_painel_estab_visto
    ON alertas_painel (estabelecimento_id, visto, criado_em DESC);