-- V1__criar_tabelas_iniciais.sql
-- Flyway executa isso automaticamente na primeira vez que o projeto sobe

CREATE TABLE IF NOT EXISTS estabelecimentos (
    id            BIGSERIAL     PRIMARY KEY,
    nome          VARCHAR(150)  NOT NULL,
    slug          VARCHAR(150)  NOT NULL UNIQUE,
    tipo          VARCHAR(50)   NOT NULL,   -- SALAO, BARBEARIA, CLINICA_ESTETICA
    plano         VARCHAR(20)   NOT NULL,   -- BASE, PREMIUM, TOP
    telefone      VARCHAR(20),
    endereco      VARCHAR(300),
    criado_em     TIMESTAMP     NOT NULL DEFAULT NOW(),
    ativo         BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS usuarios (
    id                   BIGSERIAL     PRIMARY KEY,
    nome                 VARCHAR(150)  NOT NULL,
    email                VARCHAR(150)  NOT NULL UNIQUE,
    senha                VARCHAR(255)  NOT NULL,
    role                 VARCHAR(30)   NOT NULL,   -- ADMIN, FUNCIONARIO, FUNCIONARIO_TOP
    estabelecimento_id   BIGINT        NOT NULL REFERENCES estabelecimentos(id)
);

-- Index para queries de tenant (buscar dados por estabelecimento)
CREATE INDEX idx_usuarios_estabelecimento ON usuarios(estabelecimento_id);
