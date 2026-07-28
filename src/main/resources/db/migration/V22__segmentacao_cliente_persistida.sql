-- V23: persistência do EstagioCliente + histórico de transições.
--
-- Contexto: EstagioCliente (NOVO/RECORRENTE/VIP/INATIVO) até aqui era
-- calculado 100% sob demanda por ClienteSegmentacaoService, sem nenhuma
-- coluna própria. Isso é suficiente para leituras pontuais
-- (GET /painel/clientes/{id}/estagio), mas não permite:
--   a) listar em massa todos os clientes de um estágio sem recalcular um a
--      um (necessário para os itens 9/10 de campanha/reativação por regra);
--   b) saber QUANDO um cliente mudou de estágio, só o estágio atual
--      (necessário para o item 11, funil/CRM, que reage à transição em si).
--
-- Esta migration não altera a lógica de cálculo (regras seguem em
-- ClienteSegmentacaoService), só adiciona onde persistir o resultado.
--
-- Regras de negócio confirmadas nesta data (substituindo os valores padrão
-- anteriores de 60 dias / 6 visitas):
--   - INATIVO: sem visita concluída há mais de 35 dias
--   - VIP:     10 ou mais visitas concluídas

ALTER TABLE clientes
    ADD COLUMN estagio_atual VARCHAR(20) NULL,
    ADD COLUMN estagio_atualizado_em TIMESTAMP NULL;

CREATE TABLE cliente_estagio_historico (
                                           id BIGSERIAL PRIMARY KEY,
                                           cliente_id BIGINT NOT NULL REFERENCES clientes(id),
                                           estabelecimento_id BIGINT NOT NULL REFERENCES estabelecimentos(id),
                                           estagio_anterior VARCHAR(20) NULL,
                                           estagio_novo VARCHAR(20) NOT NULL,
                                           criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_cliente_estagio_historico_cliente ON cliente_estagio_historico (cliente_id);
CREATE INDEX idx_cliente_estagio_historico_estabelecimento ON cliente_estagio_historico (estabelecimento_id);

-- Índice de suporte à futura query "todos os clientes de um estágio"
-- (item 9/10) — filtragem sempre escopada por estabelecimento.
CREATE INDEX idx_clientes_estabelecimento_estagio ON clientes (estabelecimento_id, estagio_atual);