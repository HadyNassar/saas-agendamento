-- V25__cupom_estabelecimento.sql
--
-- Cupom de desconto do DONO DO ESTABELECIMENTO para os próprios clientes
-- (seção 5-B do documento). Não confundir com a tabela `cupons` já
-- existente, que é do admin-master da plataforma (cupom de assinatura) —
-- por isso entidade/tabela nova, sem estabelecimento_id na `cupons` antiga.
--
-- Decisões confirmadas com o dono do produto para esta primeira versão:
--   - Aplicação só via link (o link do agendamento público já vem com o
--     cupom aplicado) — não existe campo de "digitar código" no formulário.
--   - Tipo de desconto: PERCENTUAL ou VALOR_FIXO, o dono escolhe por cupom.
--   - Limite de uso: fica a critério do dono por cupom — por isso
--     `validade_data` E `limite_usos` são OPCIONAIS (nulo = sem restrição
--     naquele eixo), ele configura o que quiser, inclusive nenhum dos dois.
--
-- IMPORTANTE (escopo desta migration, decisão minha não validada com o dono
-- do produto): as colunas novas em `agendamentos` só REGISTRAM qual cupom
-- foi usado e quanto de desconto foi aplicado, para fins de relatório/
-- métricas futuras (item 12). Esta migration NÃO altera o cálculo de sinal/
-- pagamento via Mercado Pago, que é um fluxo separado e não foi tocado.

CREATE TABLE cupom_estabelecimento (
                                       id BIGSERIAL PRIMARY KEY,
                                       estabelecimento_id BIGINT NOT NULL REFERENCES estabelecimentos(id),
                                       codigo VARCHAR(50) NOT NULL,
                                       tipo_desconto VARCHAR(20) NOT NULL,       -- PERCENTUAL | VALOR_FIXO
                                       valor_desconto NUMERIC(10,2) NOT NULL,
                                       validade_data DATE,                        -- NULL = sem expiração
                                       limite_usos INTEGER,                        -- NULL = ilimitado
                                       usos_atuais INTEGER NOT NULL DEFAULT 0,
                                       ativo BOOLEAN NOT NULL DEFAULT TRUE,
                                       criado_em TIMESTAMP NOT NULL DEFAULT now(),

    -- Código é único POR ESTABELECIMENTO, não globalmente — dois salões
    -- diferentes podem ambos ter um cupom "PROMO10" sem conflito.
                                       CONSTRAINT uq_cupom_estabelecimento_codigo UNIQUE (estabelecimento_id, codigo)
);

CREATE INDEX idx_cupom_estabelecimento_estab_ativo
    ON cupom_estabelecimento (estabelecimento_id, ativo);

ALTER TABLE agendamentos
    ADD COLUMN cupom_estabelecimento_id BIGINT REFERENCES cupom_estabelecimento(id),
    ADD COLUMN valor_desconto_aplicado NUMERIC(10,2);