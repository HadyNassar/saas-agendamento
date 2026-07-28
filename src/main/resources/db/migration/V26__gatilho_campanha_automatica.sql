-- V25: suporte ao gatilho automático de campanha por mudança de estágio
-- (itens 9/10 da camada PLATINUM — ver CampanhaAutomaticaService).
--
-- Marca se uma transição de estágio já foi avaliada pelo job diário que cria
-- CampanhaEnvio automaticamente para clientes que viraram VIP ou INATIVO.
-- Linhas já existentes recebem TRUE (nada a reprocessar retroativamente —
-- só passa a valer pra transições novas a partir de agora); novas linhas
-- nascem FALSE por padrão.

ALTER TABLE cliente_estagio_historico
    ADD COLUMN processado_campanha_automatica BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE cliente_estagio_historico
    ALTER COLUMN processado_campanha_automatica SET DEFAULT FALSE;