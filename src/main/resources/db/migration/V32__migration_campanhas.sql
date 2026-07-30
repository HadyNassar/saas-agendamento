-- ============================================================
-- Migration: template de campanha, filtro TODOS, anti-banimento
--            e variação de mensagem por destinatário
-- Rode isso direto no pgAdmin (Query Tool), na ordem abaixo.
-- ============================================================

-- 1) Estabelecimento: template universal + intervalo mínimo entre campanhas
ALTER TABLE estabelecimentos
    ADD COLUMN template_mensagem_campanha TEXT NULL,
    ADD COLUMN intervalo_minimo_campanha_dias INTEGER NOT NULL DEFAULT 2;

-- 2) CampanhaEnvio: guarda se o template foi usado (informativo)
ALTER TABLE campanha_envio
    ADD COLUMN usou_template BOOLEAN NOT NULL DEFAULT FALSE;

-- 3) CampanhaEnvioItem: mensagem final por destinatário (com variação) +
--    horário agendado (espaçamento anti-banimento)
ALTER TABLE campanha_envio_item
    ADD COLUMN mensagem_final TEXT NULL,
    ADD COLUMN agendado_para TIMESTAMP WITHOUT TIME ZONE NULL;

-- Nenhuma migration é necessária para o enum EstagioCliente (TODOS) porque
-- filtro_estagio na tabela campanha_envio é armazenado como VARCHAR
-- (character varying), não como um tipo ENUM nativo do Postgres — o Hibernate
-- grava a String do enum Java diretamente. Se no seu banco esse campo for de
-- fato um ENUM nativo do Postgres (confirme com o comando abaixo), aí sim
-- seria preciso rodar um ALTER TYPE adicional.

-- Comando para confirmar se filtro_estagio é enum nativo ou varchar comum
-- (se já rodou a query anterior e retornou "character varying", pode ignorar
-- este passo, já está tudo certo):
--
-- SELECT data_type, udt_name FROM information_schema.columns
-- WHERE table_name = 'campanha_envio' AND column_name = 'filtro_estagio';