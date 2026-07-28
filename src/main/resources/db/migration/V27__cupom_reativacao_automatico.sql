-- V27: cupom automático de reativação (item 8 — ver CampanhaAutomaticaService).
--
-- Permite ao dono do estabelecimento ativar, de forma opcional, o envio
-- automático de um cupom de desconto sempre que um cliente vira INATIVO
-- (junto com a mensagem de "sentimos sua falta" do gatilho de campanha
-- automática — ver V25/V26).
--
-- Desligado por padrão (cupom_reativacao_automatico_ativo = FALSE) — nunca
-- gera cupom pra ninguém sem o dono ativar explicitamente. Tipo e valor de
-- desconto ficam nulos até o dono configurar pela primeira vez
-- (CupomAutomaticoController).

ALTER TABLE estabelecimentos
    ADD COLUMN cupom_reativacao_automatico_ativo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN cupom_reativacao_tipo_desconto VARCHAR(20),      -- PERCENTUAL | VALOR_FIXO
    ADD COLUMN cupom_reativacao_valor_desconto NUMERIC(10,2);