-- Item 17 — indicação/referral automatizada (continuação da V30).
--
-- Configuração do desconto dos dois cupons pessoais gerados quando uma
-- indicação é concluída (um pro indicador, um pro indicado) — mesmo padrão
-- de PERCENTUAL | VALOR_FIXO já usado no cupom de reativação (V27).

ALTER TABLE estabelecimentos
    ADD COLUMN indicacao_tipo_desconto VARCHAR(20),
    ADD COLUMN indicacao_valor_desconto NUMERIC(10,2);