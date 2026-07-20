CREATE TABLE IF NOT EXISTS notas_internas (
                                              id BIGSERIAL PRIMARY KEY,
                                              estabelecimento_id BIGINT NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE,
    texto TEXT NOT NULL,
    criado_por VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_notas_internas_estabelecimento ON notas_internas(estabelecimento_id);

CREATE TABLE IF NOT EXISTS log_auditoria (
                                             id BIGSERIAL PRIMARY KEY,
                                             admin_email VARCHAR(255) NOT NULL,
    acao VARCHAR(100) NOT NULL,
    estabelecimento_id BIGINT,
    detalhes TEXT,
    criado_em TIMESTAMP DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_log_auditoria_estabelecimento ON log_auditoria(estabelecimento_id);
CREATE INDEX IF NOT EXISTS idx_log_auditoria_criado_em ON log_auditoria(criado_em);