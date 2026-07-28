-- V21: toggle geral do robô conversacional por estabelecimento.
-- Importante: isso NÃO afeta lembretes/confirmações automáticas (WhatsAppService),
-- que são tratados como serviço à parte e continuam rodando sempre, mesmo com o
-- robô conversacional desligado aqui.

ALTER TABLE estabelecimentos
    ADD COLUMN robot_ativo BOOLEAN NOT NULL DEFAULT TRUE;