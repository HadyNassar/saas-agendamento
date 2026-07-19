-- Permite usuários sem estabelecimento vinculado (admin master do SaaS)
ALTER TABLE usuarios ALTER COLUMN estabelecimento_id DROP NOT NULL;

-- Códigos de verificação 2FA por email, com expiração curta
CREATE TABLE IF NOT EXISTS codigo_verificacao_2fa (
                                                      id SERIAL PRIMARY KEY,
                                                      usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    codigo VARCHAR(6) NOT NULL,
    usado BOOLEAN DEFAULT FALSE,
    expira_em TIMESTAMP NOT NULL,
    criado_em TIMESTAMP DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_codigo_2fa_usuario ON codigo_verificacao_2fa(usuario_id, usado);