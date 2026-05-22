-- V4__anamnese.sql

CREATE TABLE IF NOT EXISTS perguntas_anamnese (
    id                   BIGSERIAL     PRIMARY KEY,
    texto                TEXT          NOT NULL,
    tipo                 VARCHAR(30)   NOT NULL,  -- SIM_NAO, SIM_NAO_DETALHE, TEXTO_LIVRE
    ordem                INTEGER       NOT NULL DEFAULT 0,
    ativa                BOOLEAN       NOT NULL DEFAULT TRUE,
    estabelecimento_id   BIGINT        NOT NULL REFERENCES estabelecimentos(id)
);

CREATE TABLE IF NOT EXISTS respostas_anamnese (
    id                        BIGSERIAL     PRIMARY KEY,
    cliente_id                BIGINT        NOT NULL REFERENCES clientes(id),
    pergunta_id               BIGINT        NOT NULL REFERENCES perguntas_anamnese(id),
    resposta                  TEXT          NOT NULL,  -- "SIM", "NAO", ou texto livre
    detalhe                   TEXT,                   -- detalhe quando SIM_NAO_DETALHE
    preenchido_pelo_cliente   BOOLEAN       NOT NULL DEFAULT TRUE,
    preenchido_em             TIMESTAMP     NOT NULL DEFAULT NOW(),
    editado_em                TIMESTAMP,
    UNIQUE (cliente_id, pergunta_id)  -- cliente responde cada pergunta só uma vez
);

CREATE INDEX idx_perguntas_estabelecimento ON perguntas_anamnese(estabelecimento_id);
CREATE INDEX idx_respostas_cliente ON respostas_anamnese(cliente_id);
