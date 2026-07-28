-- Item 17 — indicação/referral automatizada (ver IndicacaoService,
-- IndicacaoPainelController, tabela indicacoes criada na V29).
--
-- Opt-in por estabelecimento: só dispara o link de indicação após nota alta
-- se o dono tiver ativado a função explicitamente. Desligado por padrão —
-- mesmo padrão já usado no cupom de reativação automático (V27).

ALTER TABLE estabelecimentos
    ADD COLUMN indicacao_automatica_ativa BOOLEAN NOT NULL DEFAULT FALSE;