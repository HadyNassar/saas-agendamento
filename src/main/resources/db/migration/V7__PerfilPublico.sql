ALTER TABLE estabelecimentos
    ADD COLUMN IF NOT EXISTS foto_perfil       VARCHAR(500),
    ADD COLUMN IF NOT EXISTS instagram         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS whatsapp_contato  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS facebook          VARCHAR(200);

CREATE TABLE IF NOT EXISTS estabelecimento_comodidades (
                                                           estabelecimento_id BIGINT NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE,
    comodidade         VARCHAR(100) NOT NULL
    );

CREATE TABLE IF NOT EXISTS estabelecimento_formas_pagamento (
                                                                estabelecimento_id BIGINT NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE,
    forma_pagamento    VARCHAR(100) NOT NULL
    );