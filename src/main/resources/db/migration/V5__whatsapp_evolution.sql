-- V5__whatsapp_evolution.sql
-- Adiciona suporte à integração com Evolution API (WhatsApp)

-- Colunas na tabela estabelecimentos
ALTER TABLE estabelecimentos
    ADD COLUMN IF NOT EXISTS evolution_instance_name VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS whatsapp_conectado      BOOLEAN NOT NULL DEFAULT FALSE;

-- Tabela de log de mensagens WhatsApp enviadas
CREATE TABLE IF NOT EXISTS whatsapp_mensagens (
    id                   BIGSERIAL     PRIMARY KEY,
    estabelecimento_id   BIGINT        NOT NULL REFERENCES estabelecimentos(id),
    agendamento_id       BIGINT        REFERENCES agendamentos(id),
    cliente_id           BIGINT        REFERENCES clientes(id),
    telefone_destino     VARCHAR(20)   NOT NULL,
    mensagem             TEXT          NOT NULL,
    tipo                 VARCHAR(30)   NOT NULL DEFAULT 'NOTIFICACAO',
    -- tipos: CONFIRMACAO, LEMBRETE, CANCELAMENTO, NOTIFICACAO
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    -- status: PENDENTE, ENVIADO, ERRO
    erro_detalhe         TEXT,
    enviado_em           TIMESTAMP,
    criado_em            TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_whatsapp_estabelecimento ON whatsapp_mensagens(estabelecimento_id);
CREATE INDEX idx_whatsapp_agendamento     ON whatsapp_mensagens(agendamento_id);
CREATE INDEX idx_whatsapp_status          ON whatsapp_mensagens(status);
