-- Coluna de filtro por estágio do cliente
ALTER TABLE campanha_envio
    ADD COLUMN IF NOT EXISTS filtro_estagio VARCHAR(50);

-- FK para cupom vinculado à campanha
ALTER TABLE campanha_envio
    ADD COLUMN IF NOT EXISTS cupom_estabelecimento_id BIGINT;

ALTER TABLE campanha_envio
    ADD CONSTRAINT fk_campanha_envio_cupom_estabelecimento
        FOREIGN KEY (cupom_estabelecimento_id)
            REFERENCES cupom_estabelecimento (id);

-- Tabela de junção campanha <-> profissionais selecionados
CREATE TABLE IF NOT EXISTS campanha_envio_profissional (
                                                           campanha_envio_id BIGINT NOT NULL,
                                                           profissional_id   BIGINT NOT NULL,
                                                           PRIMARY KEY (campanha_envio_id, profissional_id),
    CONSTRAINT fk_cep_campanha FOREIGN KEY (campanha_envio_id) REFERENCES campanha_envio (id),
    CONSTRAINT fk_cep_profissional FOREIGN KEY (profissional_id) REFERENCES profissional (id)
    );