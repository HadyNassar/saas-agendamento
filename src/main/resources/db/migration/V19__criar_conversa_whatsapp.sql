-- V20: fundação do bot conversacional de WhatsApp
-- 1) Nova tabela conversa_whatsapp: uma conversa por (cliente, estabelecimento)
-- 2) Ajustes em whatsapp_mensagens para também registrar mensagens RECEBIDAS

CREATE TABLE conversa_whatsapp (
                                   id BIGSERIAL PRIMARY KEY,
                                   cliente_id BIGINT NOT NULL REFERENCES clientes(id),
                                   estabelecimento_id BIGINT NOT NULL REFERENCES estabelecimentos(id),

    -- Estado do fluxo do bot: LIVRE, AGUARDANDO_SERVICO, AGUARDANDO_PROFISSIONAL,
    -- AGUARDANDO_HORARIO, CONFIRMANDO, etc. (string livre, mesmo padrão de whatsapp_mensagens.tipo)
                                   estado VARCHAR(30) NOT NULL DEFAULT 'LIVRE',

    -- Contexto do fluxo em andamento (ex: id do serviço/profissional já escolhidos),
    -- guardado como JSON simples em texto para não precisar de novas tabelas agora
                                   contexto TEXT,

    -- Controle manual do dono do estabelecimento
                                   robot_pausado BOOLEAN NOT NULL DEFAULT FALSE,
                                   pausado_ate TIMESTAMP NULL,

                                   ultima_mensagem_em TIMESTAMP NULL,
                                   ultima_mensagem_preview VARCHAR(200),

                                   criado_em TIMESTAMP NOT NULL DEFAULT now(),
                                   atualizado_em TIMESTAMP NOT NULL DEFAULT now(),

                                   CONSTRAINT uk_conversa_cliente_estabelecimento UNIQUE (cliente_id, estabelecimento_id)
);

CREATE INDEX idx_conversa_whatsapp_estabelecimento ON conversa_whatsapp(estabelecimento_id);
CREATE INDEX idx_conversa_whatsapp_cliente ON conversa_whatsapp(cliente_id);

-- whatsapp_mensagens hoje só grava ENVIADAS (o campo "status" cobre isso).
-- Adicionamos "direcao" para também gravar RECEBIDA, e um link opcional para a conversa.
ALTER TABLE whatsapp_mensagens
    ADD COLUMN direcao VARCHAR(10) NOT NULL DEFAULT 'ENVIADA';

ALTER TABLE whatsapp_mensagens
    ADD COLUMN conversa_id BIGINT NULL REFERENCES conversa_whatsapp(id);

CREATE INDEX idx_whatsapp_mensagens_conversa ON whatsapp_mensagens(conversa_id);