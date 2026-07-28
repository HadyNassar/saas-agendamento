-- Item 2 (seção 5, camada ELITE): confirmação/cancelamento de lembrete por resposta.
-- Guarda se o cliente confirmou presença respondendo "1" ao lembrete de 24h/2h,
-- para permitir métricas de taxa de confirmação no futuro (item 12, PLATINUM).
-- O cancelamento por resposta ("2") não precisa de coluna nova: reaproveita o
-- fluxo de cancelamento já existente (status = CANCELADO).

ALTER TABLE agendamentos
    ADD COLUMN confirmado_pelo_cliente BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE agendamentos
    ADD COLUMN confirmado_em TIMESTAMP;