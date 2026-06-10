CREATE TABLE oauth_state (
                             state       VARCHAR(36) PRIMARY KEY,
                             estabelecimento_id BIGINT NOT NULL,
                             criado_em   TIMESTAMP NOT NULL
);