ALTER TABLE codigo_verificacao_2fa
ALTER COLUMN id TYPE BIGINT;

ALTER SEQUENCE IF EXISTS codigo_verificacao_2fa_id_seq AS BIGINT;