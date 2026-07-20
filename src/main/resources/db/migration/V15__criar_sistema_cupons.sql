CREATE TABLE IF NOT EXISTS cupons (
                                      id SERIAL PRIMARY KEY,
                                      codigo VARCHAR(50) NOT NULL UNIQUE,
    tipo_desconto VARCHAR(20) NOT NULL,          -- PERCENTUAL | VALOR_FIXO
    valor_desconto NUMERIC(10, 2) NOT NULL,
    plano_restrito VARCHAR(20),                  -- NULL = vale para qualquer plano
    validade_data DATE,                          -- NULL = sem data de expiração
    limite_usos INTEGER,                         -- NULL = uso ilimitado
    usos_atuais INTEGER NOT NULL DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP DEFAULT NOW()
    );

-- Registra cada uso de cupom — permite auditoria de quem usou o quê e quando,
-- além de impedir reuso indevido pelo mesmo estabelecimento.
CREATE TABLE IF NOT EXISTS cupom_usos (
                                          id SERIAL PRIMARY KEY,
                                          cupom_id BIGINT NOT NULL REFERENCES cupons(id) ON DELETE CASCADE,
    estabelecimento_id BIGINT NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE,
    usado_em TIMESTAMP DEFAULT NOW(),
    UNIQUE(cupom_id, estabelecimento_id)
    );

CREATE INDEX IF NOT EXISTS idx_cupom_usos_cupom ON cupom_usos(cupom_id);